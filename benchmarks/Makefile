J := java -jar
TARGET := target/rio-bench-1.0-SNAPSHOT-jar-with-dependencies.jar
T_RIO := org.cqfn.rio.bench.RioTarget
T_VTX := org.cqfn.rio.bench.VertxTarget
T_FLUX := org.cqfn.rio.bench.FluxTarget
TEST_FILES = test.1 test.1024 test.10240 test.102400 test.1048576
TEST_DIR = /var/tmp/rio-bench
LOG_DIR := $(PWD)/logs

.PHONY: all
all: benchmarks

$(TEST_DIR):
	mkdir -pv $(TEST_DIR)

$(LOG_DIR):
	mkdir -pv $(LOG_DIR)

$(TARGET):
	mvn clean install -f .. -DskipTests=true -P\!qulice
	mvn clean package
	rm -fr $(LOG_DIR)

define urand_test_res
	dd if=/dev/urandom of=$(TEST_DIR)/test.$(1) bs=1024 count=$(1)
endef

test.%: $(TEST_DIR)
	if [[ ! -f $(TEST_DIR)/$@ ]] ; then \
	  $(call urand_test_res,$(subst test.,,$@)); \
	fi

.PHONY: distclean
distclean: clean
	rm -vf $(TEST_DIR)/test.*

.PHONY: clean
clean:
	mvn clean

define _bench_dummy
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
endef

define _bench_read
	@echo "bench_read size=$(1) count=$(2) warmup=$(3) parallel=$(4)"
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/rio-read_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/vertx-read_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/flux-read_"$(1)_$(2)_$(4)".bench
endef

define _bench_write
	@echo "bench_write size=$(1) count=$(2) warmup=$(3) parallel=$(4)"
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) --size $(1) -d test.$(1).write.rio -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/rio-write_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) --size $(1) -d test.$(1).write.vtx -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/vertx-write_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) --size $(1) -d test.$(1).write.vtx -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/flux-write_"$(1)_$(2)_$(4)".bench
endef

define _bench_copy
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.rio -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/rio-copy_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.vtx -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/vertx-copy_"$(1)_$(2)_$(4)".bench
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.vtx -c $(2) -w $(3) -l $(4) > \
	  $(LOG_DIR)/flux-copy_"$(1)_$(2)_$(4)".bench
endef

.PHONY: bench_read
bench_read: $(TARGET) $(TEST_FILES) $(LOG_DIR)
	# Read tests
	# 1K
	$(call _bench_read,1,1000000,100,1) # 976.5625 Mb
	$(call _bench_read,1,100000,100,10) # 976.5625 Mb
	$(call _bench_read,1,10000,100,100) # 976.5625 Mb
	# 1M
	$(call _bench_read,1024,10000,100,1) # 9.765625 Gb
	$(call _bench_read,1024,1000,100,10) # 9.765625 Gb
	$(call _bench_read,1024,1000,10,100) # 97.65625 Gb
	# 10M
	$(call _bench_read,10240,1000,100,1) # 9.765625 Gb
	$(call _bench_read,10240,1000,100,10) # 97.65625 Gb
	$(call _bench_read,10240,500,10,20) # 97.65625 Gb
	# 100M
	$(call _bench_read,102400,1000,50,1) # 97.65625 Gb
	$(call _bench_read,102400,500,50,2) # 97.65625 Gb
	$(call _bench_read,102400,200,20,5) # 97.65625 Gb

.PHONY: bench_write
bench_write: $(TARGET) $(TEST_FILES) $(LOG_DIR)
	# Write tests
	# 1K
	$(call _bench_write,1,1000000,100,1) # 976.5625 Mb
	$(call _bench_write,1,100000,100,10) # 976.5625 Mb
	$(call _bench_write,1,10000,100,100) # 976.5625 Mb
	# 1M
	$(call _bench_write,1024,10000,100,1) # 9.765625 Gb
	$(call _bench_write,1024,1000,100,10) # 9.765625 Gb
	$(call _bench_write,1024,1000,10,100) # 97.65625 Gb
	# 10M
	$(call _bench_write,10240,1000,100,1) # 9.765625 Gb
	$(call _bench_write,10240,1000,100,10) # 97.65625 Gb
	$(call _bench_write,10240,500,10,20) # 97.65625 Gb
	# 100M
	$(call _bench_write,102400,1000,50,1) # 97.65625 Gb
	$(call _bench_write,102400,500,50,2) # 97.65625 Gb
	$(call _bench_write,102400,200,20,5) # 97.65625 Gb

.PHONY: bench_copy
bench_copy: $(TARGET) $(TEST_FILES) $(LOG_DIR)
	# Copy tests
	# 1K
	$(call _bench_copy,1,1000000,100,1) # 976.5625 Mb
	$(call _bench_copy,1,100000,100,10) # 976.5625 Mb
	$(call _bench_copy,1,10000,100,100) # 976.5625 Mb
	# 1M
	$(call _bench_copy,1024,10000,100,1) # 9.765625 Gb
	$(call _bench_copy,1024,1000,100,10) # 9.765625 Gb
	$(call _bench_copy,1024,1000,10,100) # 97.65625 Gb
	# 10M
	$(call _bench_copy,10240,1000,100,1) # 9.765625 Gb
	$(call _bench_copy,10240,1000,100,10) # 97.65625 Gb
	$(call _bench_copy,10240,500,10,20) # 97.65625 Gb
	# 100M
	$(call _bench_copy,102400,1000,50,1) # 97.65625 Gb
	$(call _bench_copy,102400,500,50,2) # 97.65625 Gb
	$(call _bench_copy,102400,200,20,5) # 97.65625 Gb

.PHONY: bench_all
bench_all: bench_read bench_write bench_copy

.PHONY: benchmarks
benchmarks: clean bench_all

