J := java -jar
TARGET := target/rio-bench-1.0-SNAPSHOT-jar-with-dependencies.jar
T_RIO := org.cqfn.rio.bench.RioTarget
T_VTX := org.cqfn.rio.bench.VertxTarget
T_FLUX := org.cqfn.rio.bench.FluxTarget
TEST_FILES = test.1 test.1024 test.10240 test.102400 test.1048576
TEST_DIR = /var/tmp/rio-bench

.PHONY: all clean bench_dummy bench_read bench_copy bench_write bench_all

all: benchmarks

$(TEST_DIR):
	mkdir -pv $(TEST_DIR)

$(TARGET):
	mvn clean install -f .. -DskipTests=true -P\!qulice
	mvn clean package

define urand_test_res
	dd if=/dev/urandom of=$(TEST_DIR)/test.$(1) bs=1024 count=$(1)
endef

test.%: $(TEST_DIR)
	if [[ ! -f $(TEST_DIR)/$@ ]] ; then \
	  $(call urand_test_res,$(subst test.,,$@)); \
	fi

distclean: clean
	rm -vf $(TEST_DIR)/test.*

clean:
	mvn clean

define _bench_dummy
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) --size $(1) -c $(2) -w $(3)
endef

define _bench_read
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4)
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4)
    @$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) -s test.$(1) -c $(2) -w $(3) -l $(4)
endef

define _bench_copy
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.rio -c $(2) -w $(3) -l $(4)
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.vtx -c $(2) -w $(3) -l $(4)
	@$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) -s test.$(1) -d test.$(1).cpy.vtx -c $(2) -w $(3) -l $(4)
endef

define _bench_write
	@$(J) $(TARGET) -p $(T_RIO)  --dir $(TEST_DIR) --size $(1) -d test.$(1).write.rio -c $(2) -w $(3) -l $(4)
	@$(J) $(TARGET) -p $(T_VTX)  --dir $(TEST_DIR) --size $(1) -d test.$(1).write.vtx -c $(2) -w $(3) -l $(4)
    @$(J) $(TARGET) -p $(T_FLUX) --dir $(TEST_DIR) --size $(1) -d test.$(1).write.vtx -c $(2) -w $(3) -l $(4)
endef

bench_dummy: $(TARGET) $(TEST_FILES)
	@echo "### Dummy tests"
	$(call _bench_dummy,1,1000,100)
	$(call _bench_dummy,1000,1000,100)
	$(call _bench_dummy,10000,1000,100)
	$(call _bench_dummy,100000,500,50)
	$(call _bench_dummy,1000000,100,10)

bench_read: $(TARGET) $(TEST_FILES)
	@echo "## Read tests"
	@echo "### 1K files"
	$(call _bench_read,1,1000,100,1)
	$(call _bench_read,1,1000,100,10)
	$(call _bench_read,1,1000,100,100)
	$(call _bench_read,1,1000,100,1000)

	@echo "### 1M files"
	$(call _bench_read,1024,1000,100,1)
	$(call _bench_read,1024,1000,100,10)
	$(call _bench_read,1024,100,100,100)
	$(call _bench_read,1024,10,100,1000)

	@echo "### 10M files"
	$(call _bench_read,10240,500,100,1)
	$(call _bench_read,10240,50,100,10)
	$(call _bench_read,10240,5,100,100)
	$(call _bench_read,10240,1,100,200)

	@echo "### 100M files"
	$(call _bench_read,102400,500,50,1)
	$(call _bench_read,102400,50,50,10)
	$(call _bench_read,102400,5,50,100)

	@echo "### 1G files"
	$(call _bench_read,1048576,50,10,1)
	$(call _bench_read,1048576,5,10,10)

bench_write: $(TARGET) $(TEST_FILES)
	@echo "## Write tests"
	@echo "### 1K files"
	$(call _bench_write,1,1000,100,1)
	$(call _bench_write,1,1000,100,10)
	$(call _bench_write,1,100,100,100)
	$(call _bench_write,1,100,100,1000)

	@echo "### 1M files"
	$(call _bench_write,1024,1000,100,1)
	$(call _bench_write,1024,1000,100,10)
	$(call _bench_write,1024,100,100,100)
	$(call _bench_write,1024,10,100,1000)

	@echo "### 10M files"
	$(call _bench_write,10240,500,100,1)
	$(call _bench_write,10240,50,100,10)
	$(call _bench_write,10240,5,100,100)
	$(call _bench_write,10240,1,100,200)

	@echo "### 100M files"
	$(call _bench_write,102400,500,50,1)
	$(call _bench_write,102400,50,50,10)
	$(call _bench_write,102400,5,50,100)

	@echo "### 1G files"
	$(call _bench_write,1048576,50,10,1)
	$(call _bench_write,1048576,5,10,10)


bench_copy: $(TARGET) $(TEST_FILES)
	@echo "## Copy tests"
	@echo "### 1K files"
	$(call _bench_copy,1,1000,100,1)
	$(call _bench_copy,1,1000,100,10)
	$(call _bench_copy,1,100,100,100)
	$(call _bench_copy,1,100,100,1000)

	@echo "### 1M files"
	$(call _bench_copy,1024,1000,100,1)
	$(call _bench_copy,1024,1000,100,10)
	$(call _bench_copy,1024,100,100,100)
	$(call _bench_copy,1024,100,100,1000)

	@echo "### 10M files"
	$(call _bench_copy,10240,1000,100,1)
	$(call _bench_copy,10240,1000,100,10)
	$(call _bench_copy,10240,100,100,100)
	$(call _bench_copy,10240,10,100,1000)

	@echo "### 100M files"
	$(call _bench_copy,10240,500,50,1)
	$(call _bench_copy,10240,50,50,10)
	$(call _bench_copy,10240,5,50,100)
	$(call _bench_copy,10240,1,50,200)

	@echo "### 1G files"
	$(call _bench_copy,1048576,100,10,1)
	$(call _bench_copy,1048576,100,1,10)

bench_all: bench_dummy bench_read bench_write bench_copy

benchmarks: bench_all

