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
use rand::Rng;
use rayon::prelude::*;

const GENERATIONS_SIZE: usize = 17500;
const MAX_GENERATIONS: usize = 1300;

const INFILL_RATE: f64 = 0.05;
const MUTATION_RATE: f64 = 0.5;

const MIN_ACCEPTANCE_RATE: f64 = 0.25;
const MAX_ACCEPTANCE_RATE: f64 = 0.7;
const ACCEPTANCE_RATE_GROWTH: f64 = MAX_ACCEPTANCE_RATE - MIN_ACCEPTANCE_RATE;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut ag_a = SupplierAgent::new(File::open("../data/daten3ASupplier_200.txt")?)?;
    let mut ag_b = CustomerAgent::new(File::open("../data/daten4BCustomer_200_5.txt")?)?;
    // let mut ag_a = SupplierAgent::new(File::open("../data/datenSupplier_5.txt")?)?;
    // let mut ag_b = CustomerAgent::new(File::open("../data/datenCustomer_5_3.txt")?)?;
    let med = Mediator::new(ag_a.get_contract_size(), ag_b.get_contract_size())?;

    let mut generation = med.get_random_contracts(GENERATIONS_SIZE);

    for n_generation in 0..MAX_GENERATIONS {
        let acceptance_amount = calculate_acceptance_amount(n_generation);
        let mutation_amount = calculate_mutation_amount(n_generation);

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

        while new_generation_set.len() < GENERATIONS_SIZE && loop_count < GENERATIONS_SIZE {
            let missing = (0..GENERATIONS_SIZE - new_generation_set.len());

            let mut parents: Vec<(&Contract, &Contract)> =
                Vec::with_capacity(GENERATIONS_SIZE - new_generation_set.len());

            for _ in 0..GENERATIONS_SIZE - new_generation_set.len() {
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

        while new_generation_set.len() < GENERATIONS_SIZE {
            new_generation_set
                .extend(med.get_random_contracts(GENERATIONS_SIZE - new_generation_set.len()));
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

    while generation.len() > 1 {
        if generation.len() % 2 == 0 {
            generation.remove(ag_a.vote_end(&generation));
        } else {
            generation.remove(ag_b.vote_end(&generation));
        }
    }

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
        MAX_GENERATIONS,
        GENERATIONS_SIZE,
        INFILL_RATE,
        MUTATION_RATE,
        MIN_ACCEPTANCE_RATE,
        MAX_ACCEPTANCE_RATE,
        ACCEPTANCE_RATE_GROWTH,
        ag_a.get_global_best().cost,
        ag_b.get_global_best().cost,
        ag_a.get_global_best().cost + ag_b.get_global_best().cost,
        ag_a._eval(&generation[0]),
        ag_b._eval(&generation[0]),
        ag_a._eval(&generation[0]) + ag_b._eval(&generation[0])
    );
    Ok(())
}

fn calculate_acceptance_amount(current_generation: usize) -> usize {
    let current_acceptance_amount = std::cmp::min(
        (GENERATIONS_SIZE as f64 * MAX_ACCEPTANCE_RATE) as usize,
        std::cmp::max(
            (GENERATIONS_SIZE as f64 * MIN_ACCEPTANCE_RATE) as usize,
            (GENERATIONS_SIZE as f64
                * (1.0 - (current_generation as f64 / MAX_GENERATIONS as f64))
                * ACCEPTANCE_RATE_GROWTH
                + MIN_ACCEPTANCE_RATE) as usize,
        ),
    );

    current_acceptance_amount
}

fn calculate_mutation_amount(current_generation: usize) -> usize {
    let mutation_amount = std::cmp::min(
        (GENERATIONS_SIZE as f64 * MUTATION_RATE) as usize,
        (GENERATIONS_SIZE as f64 * (current_generation as f64 / MAX_GENERATIONS as f64)) as usize,
    );

    mutation_amount
}
