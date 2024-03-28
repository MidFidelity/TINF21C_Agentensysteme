use std::io;
use rand::seq::SliceRandom;
use rand::thread_rng;
use crate::contract::Contract;

pub(crate) struct Mediator {
    contract_size: usize,
}

impl Mediator {
    pub fn new(contract_size_a: usize, contract_size_b: usize) -> Result<Self, io::Error> {
        if contract_size_a != contract_size_b {
            return Err(io::Error::new(
                io::ErrorKind::Other,
                "Verhandlung kann nicht durchgefuehrt werden, da Problemdaten nicht kompatibel",
            ));
        }
        Ok(Self {
            contract_size: contract_size_a,
        })
    }

    pub fn get_random_contracts(&self, count: usize) -> Vec<Contract> {
        let mut contracts = Vec::with_capacity(count);
        let mut rng = thread_rng();

        for _ in 0..count {
            let mut contract: Vec<usize> = (0..self.contract_size).collect();
            contract.shuffle(&mut rng);
            contracts.push(Contract {contract});
        }

        contracts
    }
}