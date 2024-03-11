use std::io;

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

    pub fn init_contract(&self) -> Vec<usize> {
        (0..self.contract_size).collect()
    }

    pub fn construct_proposal(&self, contract: &Vec<usize>) -> Vec<usize> {
        let mut proposal = contract.to_vec();
        let element = rand::random::<usize>() % (proposal.len() - 1);
        let value_1 = proposal[element];
        let value_2 = proposal[element + 1];
        proposal[element] = value_2;
        proposal[element + 1] = value_1;
        proposal
    }
}