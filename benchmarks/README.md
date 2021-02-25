
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
 VertX    | Copy    | 1M   | 1000  | 17.507898   | 2.890867    | 0.091417    |             |
 rio-v0.1 | Copy    | 10M  | 1000  | 9.418580    | 7.617681    | 0.240892    |             |
 VertX    | Copy    | 10M  | 1000  | 182.108562  | 6.272134    | 0.198342    |             |
 rio-v0.1 | Copy    | 100M | 500   | 104.309257  | 113.243445  | 5.064401    |             |
 VertX    | Copy    | 100M | 500   | 1822.104805 | 119.496880  | 5.344063    |             |
 rio-v0.1 | Copy    | 1G   | 100   | 18235.43255 | 601.134487  | 60.11344    |             |
 VertX    | Copy    | 1G   | 100   | 21893.58409 | 520.170780  | 52.017078   |             |

```
TODO: add to the table v0.3 copy benchmarks
=============== Copy benchmarks ===============
Copy 1M
RioTarget: CNT=1000 SUM=1845 MIN=0 MAX=135 AVG=1.845530 STDDEV=4.515830 STDERR=0.142803 {976.56 MB (528.63 MB/s)}
VertxTarget: CNT=1000 SUM=25574 MIN=8 MAX=763 AVG=25.574520 STDDEV=60.415667 STDERR=1.910511 {976.56 MB (38.18 MB/s)}
Copy 10M
RioTarget: CNT=1000 SUM=10485 MIN=6 MAX=141 AVG=10.485593 STDDEV=8.963004 STDERR=0.283435 {9.54 GB (931.09 MB/s)}
VertxTarget: CNT=1000 SUM=181619 MIN=74 MAX=237 AVG=181.619604 STDDEV=7.243850 STDERR=0.229071 {9.54 GB (53.77 MB/s)}
Copy 100M
RioTarget: CNT=500 SUM=45511 MIN=74 MAX=288 AVG=91.022973 STDDEV=33.117050 STDERR=1.481040 {47.68 GB (1.05 GB/s)}
VertxTarget: CNT=500 SUM=906683 MIN=1049 MAX=2479 AVG=1813.366456 STDDEV=109.127475 STDERR=4.880329 {47.68 GB (53.85 MB/s)}
Copy 1G
RioTarget: CNT=100 SUM=1740749 MIN=2434 MAX=20332 AVG=17407.491999 STDDEV=3358.381378 STDERR=335.838138 {95.37 GB (56.10 MB/s)}
VertxTarget: CNT=100 SUM=800325 MIN=7720 MAX=8195 AVG=8003.253292 STDDEV=147.134895 STDERR=14.713489 {95.37 GB (122.02 MB/s)}
```

## Usage

It's recommended to use dedicated Linux instance to run benchmarks.

TO run it on AWS EC2 instance with Amazon Linux you have to install these tools before:
 - OpenJDK11
 - Maven 3.3+
 - git

Clone `cqfn/rio` repository to EC2 instance, `cd` into `./benchmarks` dir, run `make` command, see the results.
