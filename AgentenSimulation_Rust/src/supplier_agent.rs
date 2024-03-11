use std::fs::File;
use std::io::{self, BufRead};
use crate::agent::Agent;

pub struct SupplierAgent {
    cost_matrix: Vec<Vec<isize>>,
}

impl Agent for SupplierAgent{
    fn vote(&mut self, contract: &Vec<usize>, proposal: &Vec<usize>) -> bool {
        let cost_contract = self.evaluate(contract);
        let cost_proposal = self.evaluate(proposal);
        cost_proposal < cost_contract
    }

    fn print_utility(&mut self, contract: &Vec<usize>) {
        print!("{}", self.evaluate(contract));
    }

    fn get_contract_size(&self) -> usize {
        self.cost_matrix.len()
    }
}

impl SupplierAgent {
    pub fn new(file: File) -> Result<Self, Box<dyn std::error::Error>> {
        let reader = io::BufReader::new(file);
        let mut lines = reader.lines();

        let dim = lines.next().unwrap()?.parse::<usize>()?;

        let mut cost_matrix = vec![vec![0; dim]; dim];

        for i in 0..dim {
            let line = lines.next().unwrap()?;
            let mut values = line.split_whitespace();
            for j in 0..dim {
                if let Some(val) = values.next() {
                    cost_matrix[i][j] = val.parse::<isize>()?;
                }
            }
        }

        Ok(Self { cost_matrix })
    }
    
    fn evaluate(&self, contract: &Vec<usize>) -> isize {
        let mut result = 0;
        for i in 0..contract.len() - 1 {
            let row = contract[i];
            let col = contract[i + 1];
            result += self.cost_matrix[row][col];
        }
        result
    }
}