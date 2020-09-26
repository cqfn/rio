
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

Provider | Test    | Size | Count | AVG (ms)    | STDDEV (ms) | STDERR (ms) |
:--------|:--------|-----:|------:|------------:|------------:|------------:|
 rio     | Read    | 1M   | 1000  | 1.00465     | 1.214902    | 0.038419    |
 VertX   | Read    | 1M   | 1000  | 5.22777     | 2.16942     | 0.068603    |
 rio     | Read    | 10M  | 1000  | 7.77312     | 2.16942     | 0.068603    |
 VertX   | Read    | 10M  | 1000  | 51.897785   | 1.645305    | 0.052029    |
 rio     | Read    | 100M | 500   | 56.698395   | 24.441079   | 1.093038    |
 VertX   | Read    | 100M | 500   | 540.012694  | 5.804857    | 0.259601    |
 rio     | Read    | 1G   | 100   | 568.641123  | 105.406599  | 10.540660   |
 VertX   | Read    | 1G   | 100   | 5389.446062 | 35.422950   | 3.542295    |
 rio     | Write   | 1M   | 1000  | 1.863417    | 0.554393    | 0.017531    |
 VertX   | Write   | 1M   | 1000  | 17.555435   | 4.158567    | 0.131505    |
 rio     | Write   | 10M  | 1000  | 15.551556   | 1.060045    | 0.033522    |
 VertX   | Write   | 10M  | 1000  | 181.931414  | 6.784933    | 0.214558    |
 rio     | Write   | 100M | 500   | 208.224810  | 162.662764  | 7.274500    |
 VertX   | Write   | 100M | 500   | 1822.557773 | 118.950805  | 5.319642    |
 rio     | Write   | 1G   | 100   | 4122.949523 | 5327.959935 | 532.795994  |
 VertX   | Write   | 1G   | 100   | 22208.88947 | 635.727793  | 63.572779   |
 rio     | Copy    | 1M   | 1000  | 1.424169    | 1.205526    | 0.038122    |
 VertX   | Copy    | 1M   | 1000  | 17.507898   | 2.890867    | 0.091417    |
 rio     | Copy    | 10M  | 1000  | 9.418580    | 7.617681    | 0.240892    |
 VertX   | Copy    | 10M  | 1000  | 182.108562  | 6.272134    | 0.198342    |
 rio     | Copy    | 100M | 500   | 104.309257  | 113.243445  | 5.064401    |
 VertX   | Copy    | 100M | 500   | 1822.104805 | 119.496880  | 5.344063    |
 rio     | Copy    | 1G   | 100   | 18235.43255 | 601.134487  | 60.11344    |
 VertX   | Copy    | 1G   | 100   | 21893.58409 | 520.170780  | 52.017078   |

## Usage

It's recommended to use dedicated Linux instance to run benchmarks.

TO run it on AWS EC2 instance with Amazon Linux you have to install these tools before:
 - OpenJDK11
 - Maven 3.3+
 - git

Clone `cqfn/rio` repository to EC2 instance, `cd` into `./benchmarks` dir, run `make` command, see the results.
