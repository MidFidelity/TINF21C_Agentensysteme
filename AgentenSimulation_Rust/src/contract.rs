use std::cmp::Ordering;
use std::collections::HashSet;
use std::ops::Index;
use rand::{Rng, thread_rng};

#[derive(Hash, Eq, PartialEq, Debug, Clone)]
pub struct Contract{
    pub contract:Vec<usize>
}

impl Index<usize> for Contract {
    type Output = usize;

    fn index(&self, index: usize) -> &Self::Output {
        &self.contract[index]
    }
}

impl Contract {
    pub fn mutate(&mut self) {
        let mut rng = thread_rng();
        let start = rng.gen_range(0..self.contract.len()-1);
        let end = rng.gen_range(start+1..self.contract.len());
        self.contract.swap(start, end);
    }

    pub fn ordered_crossover(&self, parent2:&Contract) -> Contract {
        let mut rng = rand::thread_rng();
        let mut child = vec![usize::MAX; self.contract.len()];
        let mut child_genes:HashSet<usize> = HashSet::with_capacity(self.len());
        // Select a random range for crossover
        let start = rng.gen_range(0..self.contract.len());
        let end = rng.gen_range(start + 1..=self.contract.len());

        // Copy the selected range from parent1 to the child
        child[start..end].clone_from_slice(&self.contract[start..end]);
        child_genes.extend(child[start..end].iter());
        // Fill in the rest of the child with elements from parent2
        let mut index = 0;
        for i in 0..child.len() {
            if !child_genes.contains(&parent2[i]) {
                while child[index] != usize::MAX {
                    index += 1;
                }
                child[index] = parent2[i];
                child_genes.insert(parent2[i]);
            }
        }

        Contract{contract:child}
    }
    
    pub(crate) fn len(&self) ->usize{
        self.contract.len()
    }
}

#[derive(Clone)]
pub struct CostContract{
    pub contract:Contract,
    pub cost:isize,
    pub index:usize
}

impl CostContract {
    pub fn new_empty(contract: Contract)->CostContract{
        CostContract{
            contract,
            cost:isize::MAX,
            index:usize::MAX
        }
    }
    
    pub fn new_filled(contract: Contract, cost:isize, index:usize)->CostContract{
        CostContract{
            contract,
            cost,
            index
        }
    }
    
    pub fn len(&self)->usize{
        self.contract.len()
    }
}

impl Index<usize> for CostContract {
    type Output = usize;

    fn index(&self, index: usize) -> &Self::Output {
        &self.contract[index]
    }
}

impl Eq for CostContract {}

impl PartialEq<Self> for CostContract {
    fn eq(&self, other: &Self) -> bool {
        self.contract == other.contract
    }
}

impl PartialOrd<Self> for CostContract {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for CostContract {
    fn cmp(&self, other: &Self) -> Ordering {
        if self.cost<other.cost { 
            Ordering::Less
        }else if self.cost>other.cost { 
            Ordering::Greater
        }else { 
            Ordering::Equal
        }
    }
}
