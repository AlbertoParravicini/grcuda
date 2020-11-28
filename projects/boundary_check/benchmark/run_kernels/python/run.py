#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
from datetime import datetime
import argparse

##############################
##############################

RESULT_FOLDER = "../../../../../data/oob/results/GTX1660"
NUM_TESTS=50

##############################
##############################

if __name__ == "__main__":
    
    parser = argparse.ArgumentParser(description="run trufflecuda kernels with boundary checks")
    
    parser.add_argument("-d", "--debug", action='store_true',
                        help="If present, print debug messages")
    parser.add_argument("-t", "--num_tests", metavar="N", type=int, default=NUM_TESTS,
                        help="Number of times each test is executed")
    
    args = parser.parse_args()
    
    now = datetime.now().strftime("%Y_%m_%d_%H_%M_%S")
    
    debug = args.debug

    opt_levels = ["O0", "O2"]
    simplify = [False, True]
    
    kernels=[
            # "axpy",
            "dot_product",
            # "convolution",
            # "hotspot",
            # "mmul",
            # "autocov",
            # "backprop",
            # "backprop2",
            "bfs",
            "gaussian",
            # "pr",
            # "hotspot3d",
            # "histogram",
            # "lud",
            # "needle",
            # "nested",

            # USED FOR SCALABILITY TEST;
            # "mmul",
            # "autocov",
            # "backprop",
            # "needle"            
            ]
    
    num_elem = {
            # "axpy": [4000000],
            # "dot_product": [1000000],
            # "convolution": [1000000],
            # "hotspot": [400**2],
            # "mmul": [120000],
            # "autocov": [500000],
            # "backprop": [200000],
            # "backprop2": [400000],
            # "bfs": [100000],
            # "gaussian": [4096],
            # "pr": [200000],
            # "hotspot3d": [128],
            # "histogram": [1000000],
            # "lud": [2400],
            # "needle": [2400],
            # "nested": [2000],

            # USED FOR SCALABILITY TEST, P100;
            # "mmul": [20000, 100000, 200000, 400000, 800000],
            # "autocov": [200_000, 400_000, 600_000, 800_000, 1_000_000], #[20000, 40000, 60000, 80000, 100000],
            # "backprop": [100_000, 200_000, 400_000, 600_000, 800_000], #[10000, 20000, 40000, 60000, 80000],
            # "needle" : [1200, 1600, 2000, 2400, 2800],
            # USED FOR SCALABILITY TEST, GTX1660;
            "dot_product": [200_000, 600_000, 1_000_000, 1_400_000, 1_800_000],
            "bfs": [80000, 120000, 160000, 200000, 400000, 600000],
            "gaussian": [1024, 2048, 4096, 6144, 9216],
            # "mmul": [40000, 60000, 80000, 100000, 120000, 200000, 400000, 800000],
            # "autocov": [100_000, 200_000, 300_000, 400_000, 500_000,  600_000, 800_000, 1_000_000, 20000000], #[20000, 40000, 60000, 80000, 100000],
            # "backprop": [50_000, 100_000, 200_000, 300_000, 400_000], #[10000, 20000, 40000, 60000, 80000],
            # "needle" : [1200, 1600, 2000, 2400, 2800]   
            }
    
    # Execute each test;
    for k in kernels:
        
        if not debug:
            # Define the output file;
            res_folder = os.path.join(RESULT_FOLDER, now)
            if not os.path.exists(res_folder):
                os.mkdir(res_folder)
            
            output_file = os.path.join(res_folder, f"{k}_{now}.csv")
            print(f"saving results to {output_file}")
            os.system(f"echo 'iteration, num_elements, opt_level, simplify, exec_time_u_k_us, exec_time_u_us, exec_time_m_k_us, exec_time_m_us, errors' > {output_file}")
            
        for o in opt_levels:
            for s in simplify:
                simplify_flag = "-s" if s else ""
                for n in num_elem[k]:
                    
                    cmd = f"graalpython --polyglot --jvm run_{k}_kernel.py -o {o} {simplify_flag} -n {n} -t {args.num_tests}"
                    
                    if debug:
                        cmd += " -d"
                    else:
                        cmd += f" >> {output_file}"
                        
                    print(f"executing {cmd}")
                    os.system(cmd)
                    
        
