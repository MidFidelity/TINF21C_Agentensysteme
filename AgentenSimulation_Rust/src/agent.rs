use crate::contract::{Contract, CostContract};

pub trait Agent {
    fn get_contract_size(&self) -> usize;
    async fn vote_many(&mut self, contracts: &Vec<Contract>, acceptance_amount: usize) -> Vec<bool>;
    async fn vote_end(&mut self, contracts: &Vec<Contract>) -> usize;
    fn _eval(&self, solution: &Contract) -> isize;
    fn get_round_best(&self) -> &CostContract;
    fn get_global_best(&self) -> &CostContract;
    async fn evaluate_async(&mut self, contracts: &Vec<Contract>) -> Vec<CostContract>;
}