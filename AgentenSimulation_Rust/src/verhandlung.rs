mod agent;
mod supplier_agent;
mod customer_agent;
mod mediator;

use std::fs::File;
use std::io::{BufRead};
use crate::agent::Agent;
use crate::supplier_agent::SupplierAgent;
use crate::customer_agent::CustomerAgent;
use crate::mediator::Mediator;


fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut ag_a = SupplierAgent::new(File::open("../data/daten3ASupplier_200.txt")?)?;
    let mut ag_b = CustomerAgent::new(File::open("../data/daten4BCustomer_200_5.txt")?)?;
    let med = Mediator::new(ag_a.get_contract_size(), ag_b.get_contract_size())?;

    let mut contract = med.init_contract();
    let max_rounds = 1_000_000;
    let max_unchanged_rounds = 1000;

    ausgabe(&mut ag_a, &mut ag_b, 0, &contract);
    
    let mut last_accepted= contract.clone();
    let mut unchanged = 0;
    for round in 1..max_rounds {
        let proposal = med.construct_proposal(&contract);
        let vote_a = ag_a.vote(&contract, &proposal);
        let vote_b = ag_b.vote(&contract, &proposal);
        if vote_a && vote_b {
            contract = proposal;
            ausgabe(&mut ag_a, &mut ag_b, round, &contract);
            unchanged = 0;
            last_accepted = contract.clone();
        }else { unchanged += 1; }
        if unchanged>=max_unchanged_rounds { 
            return Ok(()) 
        }
    }
    println!("{:?}",last_accepted);
    Ok(())
}

fn ausgabe(a1: &mut impl Agent, a2: &mut impl Agent, i:i32, contract:&Vec<usize>){
    print!("{} -> ", i);
    a1.print_utility(&contract);
    print!(" ");
    a2.print_utility(&contract);
    println!()

}
