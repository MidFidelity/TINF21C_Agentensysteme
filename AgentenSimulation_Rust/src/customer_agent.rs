use crate::agent::Agent;
use std::fs::File;
use std::io::{self, BufRead};
use std::collections::hash_map::HashMap;
use crate::contract::{Contract, CostContract};
use rayon::prelude::*;

pub struct CustomerAgent {
    time_matrix: Vec<Vec<isize>>,
    calculated_times: HashMap<Contract, isize>,
    best_round: Option<CostContract>,
    best_global: Option<CostContract>,
}

impl Agent for CustomerAgent {
    fn get_contract_size(&self) -> usize {
        self.time_matrix.len()
    }

    async fn vote_many(&mut self, contracts: &Vec<Contract>, acceptance_amount: usize) -> Vec<bool> {
        self.best_round = None;

        let sorted_cost_contracts = self.evaluate_async(contracts).await;

        let mut result: Vec<bool> = vec![false; contracts.len()];

        for i in 0..acceptance_amount {
            result[sorted_cost_contracts.get(i).unwrap().index] = true;
        }

        result
    }

    async fn vote_end(&mut self, contracts: &Vec<Contract>) -> usize {
        let sorted_cost_contracts = self.evaluate_async(contracts).await;

        if self.calculated_times.is_empty(){
            for contract in sorted_cost_contracts.iter() {
                self.calculated_times.insert(contract.contract.clone(), contract.cost);
            }
        }

        sorted_cost_contracts.last().unwrap().index
    }

    fn _eval(&self, contract: &Contract) -> isize {
        if let Some(cost) = self.calculated_times.get(contract) {
            return *cost;
        }

        let anz_m = self.time_matrix[0].len();
        assert_eq!(self.time_matrix.len(), contract.len());

        let mut start = vec![vec![0isize; anz_m]; self.time_matrix.len()];

        let mut job = contract[0];
        for m in 1..anz_m {
            start[job][m] =
                start[job][m - 1] + self.time_matrix[job][m - 1];
        }

        for j in 1..contract.len() {
            let mut delay = 0;
            let vorg = contract[j - 1];
            job = contract[j];
            let mut delay_erhoehen;

            loop {
                delay_erhoehen = false;
                start[job][0] =
                    start[vorg][0] + self.time_matrix[vorg][0] + delay;

                for m in 1..anz_m {
                    start[job][m] =
                        start[job][m - 1] + self.time_matrix[job][m - 1];
                    if start[job][m]
                        < start[vorg][m] + self.time_matrix[vorg][m]
                    {
                        delay_erhoehen = true;
                        delay += 1;
                        break;
                    }
                }

                if !delay_erhoehen {
                    break;
                }
            }
        }

        let last = contract[contract.len() - 1];

        let final_time = start[last][anz_m - 1] + self.time_matrix[last][anz_m - 1];

        final_time
    }

    async fn evaluate_async(&mut self, contracts: &Vec<Contract>) -> Vec<CostContract> {

        let costs: Vec<isize> = contracts.par_iter().map(|contract| self._eval(contract)).collect();

        let mut cost_contracts: Vec<CostContract> = Vec::new();

        for (index, contract) in contracts.iter().enumerate() {
            let cost_contract = CostContract::new_filled(contract.clone(), costs[index], index);
            cost_contracts.push(cost_contract);
            //self.calculated_times.insert(contract.clone(), costs[index]);
        }

        cost_contracts.par_sort();

        let round_best = cost_contracts.first().unwrap();
        self.best_round = Some(round_best.clone());
        if self.best_global == None || round_best<self.best_global.as_ref().unwrap() {
            self.best_global = Some(round_best.clone())
        }
        
        cost_contracts
    }

    fn get_round_best(&self) -> &CostContract {
        &self.best_round.as_ref().unwrap()
    }


    fn get_global_best(&self) -> &CostContract {
        &self.best_global.as_ref().unwrap()
    }
}

impl CustomerAgent {
    pub fn new(file: File) -> Result<Self, Box<dyn std::error::Error>> {
        let reader = io::BufReader::new(file);
        let mut lines = reader.lines();

        let jobs = lines.next().unwrap()?.parse::<usize>()?;
        let machines = lines.next().unwrap()?.parse::<usize>()?;

        let mut time_matrix = vec![vec![0; machines]; jobs];

        for i in 0..jobs {
            let line = lines.next().unwrap()?;
            let mut values = line.split_whitespace();
            for j in 0..machines {
                if let Some(val) = values.next() {
                    time_matrix[i][j] = val.parse::<isize>()?;
                }
            }
        }

        Ok(Self { time_matrix, calculated_times: HashMap::new(), best_global: None, best_round: None })
    }
}
