use std::fs::File;
use std::io::{self, BufRead};
use crate::agent::Agent;
use crate::contract::{Contract, CostContract};

pub struct SupplierAgent {
    cost_matrix: Vec<Vec<isize>>,
    best_round: Option<CostContract>,
    best_global: Option<CostContract>
}

impl Agent for SupplierAgent{
    
    fn get_contract_size(&self) -> usize {
        self.cost_matrix.len()
    }

    fn vote_many(&mut self, contracts: &Vec<Contract>, acceptance_amount:usize) -> Vec<bool> {
        let mut cost_contracts: Vec<CostContract> = Vec::new();
        self.best_round = None;
        for (i, contract) in contracts.iter().enumerate() {
            let cost_contract = CostContract{contract:contract.clone(), cost:self._evaluate(&contract), index:i};
            if self.best_round == None || cost_contract < *self.best_round.as_ref().unwrap() { 
                self.best_round = Some(cost_contract.clone());
            }
            if self.best_global == None || cost_contract < *self.best_global.as_ref().unwrap() { 
                self.best_global = Some(cost_contract.clone());
            }

            cost_contracts.push(cost_contract);
        }

        cost_contracts.sort();

        let mut result:Vec<bool> = vec![false;contracts.len()];

        for i in 0..acceptance_amount {
            result[cost_contracts.get(i).unwrap().index] = true;
        }

        result
    }

    fn vote_end(&mut self, contracts: &Vec<Contract>) -> usize {
        let mut cost_contracts: Vec<CostContract> = Vec::new();
        self.best_round = None;
        for (i, contract) in contracts.iter().enumerate() {
            let cost_contract = CostContract{contract:contract.clone(), cost:self._evaluate(&contract), index:i};
            cost_contracts.push(cost_contract);
        }
        
        cost_contracts.sort();
        
        cost_contracts.last().unwrap().index
    }

    fn _evaluate(&mut self, contract: &Contract) -> isize {
        self._eval(contract)
    }

    fn _eval(&self, contract: &Contract) -> isize {
        let mut result = 0;
        for i in 0..contract.len() - 1 {
            let row = contract[i];
            let col = contract[i + 1];
            result += self.cost_matrix[row][col];
        }
        result
    }

    fn get_round_best(&self) -> &CostContract {
        &self.best_round.as_ref().unwrap()
    }

 
    fn get_global_best(&self) -> &CostContract   {
        &self.best_global.as_ref().unwrap()
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

        Ok(Self { cost_matrix, best_round:None, best_global:None})
    }
}