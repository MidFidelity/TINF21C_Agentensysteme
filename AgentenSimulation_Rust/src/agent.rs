pub trait Agent{
    fn vote(&mut self, contract:&Vec<usize>, proposal: &Vec<usize>)->bool;
    fn print_utility(&mut self, contract: &Vec<usize>);
    fn get_contract_size(&self) ->usize;

}