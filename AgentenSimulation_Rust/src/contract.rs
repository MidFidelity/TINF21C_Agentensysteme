use std::cmp::Ordering;
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

        // Select a random range for crossover
        let start = rng.gen_range(0..self.contract.len());
        let end = rng.gen_range(start + 1..=self.contract.len());

        // Copy the selected range from parent1 to the child
        child[start..end].clone_from_slice(&self.contract[start..end]);

        // Fill in the rest of the child with elements from parent2
        let mut index = 0;
        for i in 0..child.len() {
            if !child.contains(&parent2[i]) {
                while child[index] != usize::MAX {
                    index += 1;
                }
                child[index] = parent2[i];
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
