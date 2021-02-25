
## Results


All results were calculated in nanoseconds and converted to millis then.
Each benchmark run has warm-up stage = `Count/10`.

Targets: `make` command generates random files with different sizes (1M, 10M, 100M and 1G)
to use for tests. File sizes are not fair, it's 1KB block multiplied by count factor:
 - 1M   = 1000 * 1024
 - 10M  = 10000 * 1024
 - 100M = 100000 * 1024
 - 1G = 1000000 * 1024

Benchmarks tests are:
 - `Read` - read file from disk as `Publisher<ByteBuffer>` and swallow data with empty consumer
 - `Write` - generate `Publisher<ByteBuffer>` programmatically (creating `byte[1024]` source array only once before tests)
 and write this pubisher to disk
 - `Copy` - read file from disk as `Publisher<ByteBuffer>` and write this publisher to another file async

Benchmarks results from AWS EC2 `m4.large` with 40GB SSD io2 20000 IOPS:

Provider  | Test    | Size | Count | AVG (ms)    | STDDEV (ms) | STDERR (ms) | Speed       |
:---------|:--------|-----:|------:|------------:|------------:|------------:|------------:|
 rio-v0.1 | Read    | 1M   | 1000  | 1.00465     | 1.214902    | 0.038419    |             |
 rio-v0.3 | Read    | 1M   | 1000  | 1.113888    | 0.610152    | 0.019295    | 875.33 MB/s |
 VertX    | Read    | 1M   | 1000  | 5.351578    |  1.640794   | 0.051886    | 182.42 MB/s |
 rio-v0.1 | Read    | 10M  | 1000  | 7.77312     | 2.16942     | 0.068603    |             |
 rio-v0.3 | Read    | 10M  | 1000  | 6.082703    | 7.843085    | 0.248020    | 1.57 GB/s   |
 VertX    | Read    | 10M  | 1000  | 59.108805   | 2.216381    | 0.070088    | 165.20 MB/s |
 rio-v0.1 | Read    | 100M | 500   | 56.698395   | 24.441079   | 1.093038    |             |
 rio-v0.3 | Read    | 100M | 500   | 64.400775   | 24.168890   | 1.080866    | 1.48 GB/s   |
 VertX    | Read    | 100M | 500   | 600.929453  | 8.076904    | 0.361210    | 162.51 MB/s |
 rio-v0.1 | Read    | 1G   | 100   | 568.641123  | 105.406599  | 10.540660   |             |
 rio-v0.3 | Read    | 1G   | 100   | 685.976530  | 111.756566  | 11.175657   | 1.39 GB/s   |
 VertX    | Read    | 1G   | 100   | 5972.754686 | 42.775365   | 4.277536    | 163.50 MB/s |
 rio-v0.1 | Write   | 1M   | 1000  | 1.863417    | 0.554393    | 0.017531    |             |
 rio-v0.3 | Write   | 1M   | 1000  | 1.851040    | 0.695987    | 0.022009    | 527.08 MB/s |
 VertX    | Write   | 1M   | 1000  | 17.741741   | 3.609380    | 0.114139    | 55.04 MB/s  |
 rio-v0.1 | Write   | 10M  | 1000  | 15.551556   | 1.060045    | 0.033522    |             |
 rio-v0.3 | Write   | 10M  | 1000  | 15.494070   | 1.887526    | 0.059689    | 630.18 MB/s |
 VertX    | Write   | 10M  | 1000  | 181.584343  | 8.273564    | 0.261633    | 53.78 MB/s  |
 rio-v0.1 | Write   | 100M | 500   | 208.224810  | 162.662764  | 7.274500    |             |
 rio-v0.3 | Write   | 100M | 500   | 176.186770  | 20.108135   | 0.899263    | 554.26 MB/s |
 VertX    | Write   | 100M | 500   | 1813.158240 | 92.115434   | 4.119527    | 53.86 MB/s  |
 rio-v0.1 | Write   | 1G   | 100   | 4122.949523 | 5327.959935 | 532.795994  |             |
 rio-v0.3 | Write   | 1G   | 100   | 3046.037934 | 2623.388341 | 262.338834  | 320.60 MB/s |
 VertX    | Write   | 1G   | 100   | 7744.611679 | 178.417192  | 17.841719   | 126.10 MB/s |
 rio-v0.1 | Copy    | 1M   | 1000  | 1.424169    | 1.205526    | 0.038122    |             |
 rio-v0.3 | Copy    | 1M   | 1000  | 1.845530    | 4.515830    | 0.142803    | 528.63 MB/s |
 VertX    | Copy    | 1M   | 1000  | 25.574520   | 60.415667   | 1.910511    | 38.18 MB/s  |
 rio-v0.1 | Copy    | 10M  | 1000  | 9.418580    | 7.617681    | 0.240892    |             |
 roi-v0.3 | Copy    | 10M  | 1000  | 10.485593   | 8.963004    | 0.283435    | 931.09 MB/s |
 VertX    | Copy    | 10M  | 1000  | 181.619604  | 7.243850    | 0.229071    | 53.77 MB/s  |
 rio-v0.1 | Copy    | 100M | 500   | 104.309257  | 113.243445  | 5.064401    |             |
 rio-v0.3 | Copy    | 100M | 500   | 91.022973   | 33.117050   | 1.481040    | 1.05 GB/s   |
 VertX    | Copy    | 100M | 500   | 1813.366456 | 109.127475  | 4.880329    | 53.85 MB/s  |
 rio-v0.1 | Copy    | 1G   | 100   | 18235.43255 | 601.134487  | 60.11344    |             |
 rio-v0.3 | Copy    | 1G   | 100   | 17407.49199 | 3358.381378 | 335.838138  | 56.10 MB/s  |
 VertX    | Copy    | 1G   | 100   | 8003.253292 | 147.134895  | 14.713489   | 122.02 MB/s |

## Usage

It's recommended to use dedicated Linux instance to run benchmarks.

TO run it on AWS EC2 instance with Amazon Linux you have to install these tools before:
 - OpenJDK11
 - Maven 3.3+
 - git

Clone `cqfn/rio` repository to EC2 instance, `cd` into `./benchmarks` dir, run `make` command, see the results.
