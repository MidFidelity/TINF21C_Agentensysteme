use crate::agent::Agent;
use crate::contract::{Contract, CostContract};
use rayon::iter::IntoParallelRefIterator;
use rayon::prelude::ParallelSliceMut;
use rayon::prelude::*;
use std::collections::{HashMap, HashSet};
use std::fs::File;
use std::io::{self, BufRead};

pub struct SupplierAgent {
    cost_matrix: Vec<Vec<isize>>,
    best_round: Option<CostContract>,
    best_global: Option<CostContract>,
    calculated_times: HashMap<Contract, isize>,
}

impl SupplierAgent {
    pub(crate) fn get_contract_size(&self) -> usize {
        self.cost_matrix.len()
    }

    pub(crate) fn vote_many(&mut self, contracts: &Vec<Contract>, acceptance_amount: usize) -> Vec<bool> {
        self.best_round = None;

        let sorted_cost_contracts = self.evaluate_async(contracts);

        let mut result: Vec<bool> = vec![false; contracts.len()];

        for i in 0..acceptance_amount {
            result[sorted_cost_contracts.get(i).unwrap().index] = true;
        }

        result
    }

    pub(crate) fn vote_end(&mut self, contracts: &HashSet<Contract>) -> Contract {
        let sorted_cost_contracts: Vec<CostContract> = self.evaluate_async_set(contracts);

        if self.calculated_times.is_empty() {
            for contract in sorted_cost_contracts.iter() {
                self.calculated_times
                    .insert(contract.contract.clone(), contract.cost);
            }
        }

        sorted_cost_contracts.last().unwrap().contract.clone()
    }

    pub(crate) fn _eval(&self, contract: &Contract) -> isize {
        if let Some(cost) = self.calculated_times.get(contract) {
            return *cost;
        }

        let mut result = 0;
        for i in 0..contract.len() - 1 {
            let row = contract[i];
            let col = contract[i + 1];
            result += self.cost_matrix[row][col];
        }
        result
    }

    fn evaluate_sync(&mut self, contracts: &Vec<Contract>) -> Vec<CostContract> {
        let costs: Vec<isize> = contracts
            .iter()
            .map(|contract| self._eval(contract))
            .collect();

        let mut cost_contracts: Vec<CostContract> = Vec::new();

        for (index, contract) in contracts.iter().enumerate() {
            let cost_contract = CostContract::new_filled(contract.clone(), costs[index], index);
            cost_contracts.push(cost_contract);
            //self.calculated_times.insert(contract.clone(), costs[index]);
        }

        cost_contracts.par_sort();

        let round_best = cost_contracts.first().unwrap();
        self.best_round = Some(round_best.clone());
        if self.best_global == None || round_best < self.best_global.as_ref().unwrap() {
            self.best_global = Some(round_best.clone())
        }

        cost_contracts
    }

    fn evaluate_sync_set(&mut self, contracts: &HashSet<Contract>) -> Vec<CostContract> {
        let costs: Vec<isize> = contracts
            .iter()
            .map(|contract| self._eval(contract))
            .collect();

        let mut cost_contracts: Vec<CostContract> = Vec::new();

        for (index, contract) in contracts.iter().enumerate() {
            let cost_contract = CostContract::new_filled(contract.clone(), costs[index], index);
            cost_contracts.push(cost_contract);
            //self.calculated_times.insert(contract.clone(), costs[index]);
        }

        cost_contracts.par_sort();

        let round_best = cost_contracts.first().unwrap();
        self.best_round = Some(round_best.clone());
        if self.best_global == None || round_best < self.best_global.as_ref().unwrap() {
            self.best_global = Some(round_best.clone())
        }

        cost_contracts
    }

    fn evaluate_async(&mut self, contracts: &Vec<Contract>) -> Vec<CostContract> {
        let costs: Vec<isize> = contracts
            .par_iter()
            .map(|contract| self._eval(contract))
            .collect();

        let mut cost_contracts: Vec<CostContract> = Vec::new();

        for (index, contract) in contracts.iter().enumerate() {
            cost_contracts.push(CostContract::new_filled(
                contract.clone(),
                costs[index],
                index,
            ));
        }

        cost_contracts.par_sort();

        let round_best = cost_contracts.first().unwrap();
        self.best_round = Some(round_best.clone());
        if self.best_global == None || round_best < self.best_global.as_ref().unwrap() {
            self.best_global = Some(round_best.clone())
        }

        cost_contracts
    }

    fn evaluate_async_set(&mut self, contracts: &HashSet<Contract>) -> Vec<CostContract> {
        let costs: Vec<isize> = contracts
            .par_iter()
            .map(|contract| self._eval(contract))
            .collect();

        let mut cost_contracts: Vec<CostContract> = Vec::new();

        for (index, contract) in contracts.iter().enumerate() {
            cost_contracts.push(CostContract::new_filled(
                contract.clone(),
                costs[index],
                index,
            ));
        }

        cost_contracts.par_sort();

        let round_best = cost_contracts.first().unwrap();
        self.best_round = Some(round_best.clone());
        if self.best_global == None || round_best < self.best_global.as_ref().unwrap() {
            self.best_global = Some(round_best.clone())
        }

        cost_contracts
    }

    pub(crate) fn get_round_best(&self) -> &CostContract {
        &self.best_round.as_ref().unwrap()
    }

    pub(crate) fn get_global_best(&self) -> &CostContract {
        &self.best_global.as_ref().unwrap()
    }

    pub fn new_from_file(file: File) -> Result<Self, Box<dyn std::error::Error>> {
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

        Ok(Self {
            cost_matrix,
            best_round: None,
            best_global: None,
            calculated_times: HashMap::new(),
        })
    }

    pub fn new_from_str(definition: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let mut lines = definition.lines();

        let dim = lines.next().unwrap().parse::<usize>()?;

        let mut cost_matrix = vec![vec![0; dim]; dim];

        for i in 0..dim {
            let line = lines.next().unwrap();
            let mut values = line.split_whitespace();
            for j in 0..dim {
                if let Some(val) = values.next() {
                    cost_matrix[i][j] = val.parse::<isize>()?;
                }
            }
        }

        Ok(Self {
            cost_matrix,
            best_round: None,
            best_global: None,
            calculated_times: HashMap::new(),
        })
    }
}
