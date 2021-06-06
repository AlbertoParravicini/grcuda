#pragma once
#include "benchmark.cuh"

class Benchmark11 : public Benchmark
{
public:
    Benchmark11(Options &options) : Benchmark(options) {}
    void alloc();
    void init();
    void reset();
    void execute_sync(int iter);
    void execute_async(int iter);
    void execute_cudagraph(int iter);
    void execute_cudagraph_manual(int iter);
    void execute_cudagraph_single(int iter);
    void prefetch(cudaStream_t &s1, cudaStream_t &s2);
    std::string print_result(bool short_form = false);

private:
    float *x, *y, *x1, *y1, *res;
    float *xd, *yd, *x1d, *y1d, *y1dd, *resd;
    cudaStream_t s1, s2;
};