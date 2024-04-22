mod agent;
mod contract;
mod customer_agent;
mod mediator;
mod supplier_agent;

use std::collections::HashSet;
use std::fs::File;

use crate::agent::Agent;
use crate::contract::Contract;
use crate::customer_agent::CustomerAgent;
use crate::mediator::Mediator;
use crate::supplier_agent::SupplierAgent;
use clap::Parser;
use rand::Rng;
use rayon::prelude::*;

static AGA_DEF:&str = include_str!("../../data/daten3ASupplier_200.txt");
static AGB_DEF:&str = include_str!("../../data/daten4BCustomer_200_5.txt");

#[derive(Parser)]
struct Cli {
    #[arg(long = "gen-size", default_value_t = 1000)]
    generation_size: usize,
    #[arg(long = "max-gen", default_value_t = 3000)]
    max_generations: usize,
    #[arg(long = "infill-rate", default_value_t = 0.05)]
    infill_rate: f64,
    #[arg(long = "mutation-rate", default_value_t = 0.5)]
    mutation_rate: f64,
    #[arg(long = "min-acc-rate", default_value_t = 0.15)]
    min_acceptance_rate: f64,
    #[arg(long = "max-acc-rate", default_value_t = 0.45)]
    max_acceptance_rate: f64,
}

struct Config {
    generation_size: usize,
    max_generations: usize,
    infill_rate: f64,
    mutation_rate: f64,
    min_acceptance_rate: f64,
    max_acceptance_rate: f64,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Cli::parse();

    let config = Config {
        generation_size: args.generation_size,
        max_generations: args.max_generations,
        infill_rate: args.infill_rate,
        mutation_rate: args.mutation_rate,
        min_acceptance_rate: args.min_acceptance_rate,
        max_acceptance_rate: args.max_acceptance_rate,
    };
    let mut ag_a = SupplierAgent::new_from_str(&AGA_DEF)?;
    let mut ag_b = CustomerAgent::new_from_str(&AGB_DEF)?;
    
    // let mut ag_a = SupplierAgent::new_from_file(File::open("../data/daten3ASupplier_200.txt")?)?;
    // let mut ag_b = CustomerAgent::new_from_file(File::open("../data/daten4BCustomer_200_5.txt")?)?;
    // let mut ag_a = SupplierAgent::new(File::open("../data/datenSupplier_5.txt")?)?;
    // let mut ag_b = CustomerAgent::new(File::open("../data/datenCustomer_5_3.txt")?)?;
    let med = Mediator::new(ag_a.get_contract_size(), ag_b.get_contract_size())?;

    let mut generation = med.get_random_contracts(config.generation_size);

    for n_generation in 0..config.max_generations {
        let acceptance_amount = calculate_acceptance_amount(n_generation, &config);
        let mutation_amount = calculate_mutation_amount(n_generation, &config);

        let vote_a = ag_a.vote_many(&generation, acceptance_amount);

        let vote_b = ag_b.vote_many(&generation, acceptance_amount);

        let mut intersect: Vec<Contract> = Vec::new();

        for i in 0..vote_a.len() {
            if vote_a[i] && vote_b[i] {
                intersect.push(generation[i].clone())
            }
        }

        let mut new_generation_set: HashSet<Contract> = HashSet::new();
        let mut loop_count = 0usize;
        let mut rng = rand::thread_rng();

        while new_generation_set.len() < config.generation_size
            && loop_count < config.generation_size
        {
            let missing = (0..config.generation_size - new_generation_set.len());

            let mut parents: Vec<(&Contract, &Contract)> =
                Vec::with_capacity(config.generation_size - new_generation_set.len());

            for _ in 0..config.generation_size - new_generation_set.len() {
                let parent1 = &intersect[rng.gen_range(0..intersect.len())];
                let parent2 = &intersect[rng.gen_range(0..intersect.len())];
                parents.push((parent1, parent2))
            }

            let new_children: Vec<Contract> = missing
                .into_par_iter()
                .map(|i| {
                    let (parent1, parent2) = parents[i];
                    parent1.ordered_crossover(parent2)
                })
                .collect();

            new_generation_set.extend(new_children);

            loop_count += 1;
        }

        while new_generation_set.len() < config.generation_size {
            new_generation_set.extend(
                med.get_random_contracts(config.generation_size - new_generation_set.len()),
            );
        }

        let mut new_generation: Vec<Contract> = new_generation_set.into_iter().collect();

        for _ in 0..mutation_amount {
            let i_mutate: usize = rng.gen_range(0..new_generation.len());
            new_generation[i_mutate].mutate();
        }

        generation = new_generation;

        println!(
            "{:>4}: Best A: {:>5} \t Best B: {:>5} \t AccAmount: {:>4} \t Intersect: {:>4} \t",
            n_generation,
            ag_a.get_round_best().cost,
            ag_b.get_round_best().cost,
            acceptance_amount,
            intersect.len()
        );
    }

    println!("----------\nChanging to Terminal Phase\n----------");
    
    
    let mut generation_set :HashSet<Contract>= HashSet::from_iter(generation.into_iter());

    
    while generation_set.len() > 1 {
        if generation_set.len() % 2 == 0 {
            generation_set.remove(&ag_a.vote_end(&generation_set));
        } else {
            generation_set.remove(&ag_b.vote_end(&generation_set));
        }
        print!("\x1b[2K \x1b[1G Remaining Contracts: {}", generation_set.len())
    }
    
    let result = generation_set.iter().next().unwrap();
    
    
    println!();
    println!(
        r#"----------
Config
----------

Config:
MaxGenerations:    {:5 }    GenerationSize:    {}
InfillRate:        {:.3}    MutationRate:      {:.3}
MinAcceptanceRate: {:.3}    MaxAcceptanceRate: {:.3}    AcceptanceRateGrowth: {:.3}

----------
Result
----------

Best Cost A: {}
Best Cost B: {}
Sum: {}

Picked Contract:
A: {}    B: {}
Sum: {}"#,
        config.max_generations,
        config.generation_size,
        config.infill_rate,
        config.mutation_rate,
        config.min_acceptance_rate,
        config.max_acceptance_rate,
        config.max_acceptance_rate - config.min_acceptance_rate,
        ag_a.get_global_best().cost,
        ag_b.get_global_best().cost,
        ag_a.get_global_best().cost + ag_b.get_global_best().cost,
        ag_a._eval(result),
        ag_b._eval(result),
        ag_a._eval(result) + ag_b._eval(result)
    );
    Ok(())
}

fn calculate_acceptance_amount(current_generation: usize, config: &Config) -> usize {
    let current_acceptance_amount = std::cmp::min(
        (config.generation_size as f64 * config.max_acceptance_rate) as usize,
        std::cmp::max(
            (config.generation_size as f64 * config.min_acceptance_rate) as usize,
            (config.generation_size as f64
                * (1.0 - (current_generation as f64 / config.max_generations as f64))
                * config.max_acceptance_rate
                - config.min_acceptance_rate
                + config.min_acceptance_rate) as usize,
        ),
    );

    current_acceptance_amount
}

fn calculate_mutation_amount(current_generation: usize, config: &Config) -> usize {
    let mutation_amount = std::cmp::min(
        (config.generation_size as f64 * config.mutation_rate) as usize,
        (config.generation_size as f64
            * (current_generation as f64 / config.max_generations as f64)) as usize,
    );

    mutation_amount
}
