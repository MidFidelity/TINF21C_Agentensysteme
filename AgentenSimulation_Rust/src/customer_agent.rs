use crate::agent::Agent;
use std::fs::File;
use std::io::{self, BufRead};
use std::collections::hash_map::HashMap;

pub struct CustomerAgent {
    time_matrix: Vec<Vec<isize>>,
    calculated_times: HashMap<Vec<usize>, isize>
}

impl Agent for CustomerAgent {
    fn vote(&mut self, contract: &Vec<usize>, proposal: &Vec<usize>) -> bool {
        let time_contract = self.evaluate_new(contract);
        let time_proposal = self.evaluate_new(proposal);
        time_proposal < time_contract
    }

    fn print_utility(&mut self, contract: &Vec<usize>) {
        print!("{}", self.evaluate_new(contract));
    }

    fn get_contract_size(&self) -> usize {
        self.time_matrix.len()
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

        Ok(Self { time_matrix, calculated_times: HashMap::new() })
    }

    fn evaluate_new(&mut self, solution: &Vec<usize>) -> isize {
        if let Some(&result) = self.calculated_times.get(solution) {
            return result;
        }
        
        let anz_m = self.time_matrix[0].len();
        assert_eq!(self.time_matrix.len(), solution.len());

        let mut start = vec![vec![0isize; anz_m]; self.time_matrix.len()];

        let mut job = solution[0];
        for m in 1..anz_m {
            start[job][m] =
                start[job][m - 1] + self.time_matrix[job][m - 1];
        }

        for j in 1..solution.len() {
            let mut delay = 0;
            let vorg = solution[j - 1];
            job = solution[j];
            let mut delay_erhoehen;

            loop {
                delay_erhoehen = false;
                start[job][0] =
                    start[vorg][0] + self.time_matrix[vorg][0] + delay;

                for m in 1..anz_m {
                    start[job ][m] =
                        start[job ][m - 1] + self.time_matrix[job ][m - 1];
                    if start[job ][m]
                        < start[vorg ][m] + self.time_matrix[vorg ][m]
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

        let last = solution[solution.len() - 1];

        let final_time = start[last][anz_m - 1] + self.time_matrix[last][anz_m - 1];

        self.calculated_times.insert(solution.clone(), final_time);

        final_time
    }
}
