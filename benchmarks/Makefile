J := java -jar
TARGET := target/rio-bench-1.0-SNAPSHOT-jar-with-dependencies.jar
WMP := 10
T_RIO := org.cqfn.rio.bench.RioTarget
T_VTX := org.cqfn.rio.bench.VertxTarget
TEST_FILES = test.1000 test.10000 test.100000 test.1000000

.PHONY: all clean bench_dummy bench_read bench_copy bench_write bench_all

all: benchmarks

$(TARGET):
	mvn clean install -f .. -DskipTests=true -P\!qulice
	mvn clean package

define urand_test_res
	dd if=/dev/urandom of=test.$(1)  bs=1024 count=$(1)
endef

test.%:
	$(call urand_test_res,$(subst test.,,$@))

clean:
	rm -vf test.*
	mvn clean

define _bench_dummy
	@$(J) $(TARGET) -p $(T_RIO) --size $(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX) --size $(1) -c $(2) -w $(3)
endef

define _bench_read
	@$(J) $(TARGET) -p $(T_RIO) -s test.$(1) -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX) -s test.$(1) -c $(2) -w $(3)
endef

define _bench_copy
	@$(J) $(TARGET) -p $(T_RIO) -s test.$(1) -d test.$(1).cpy.rio -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX) -s test.$(1) -d test.$(1).cpy.vtx -c $(2) -w $(3)
endef

define _bench_write
	@$(J) $(TARGET) -p $(T_RIO) --size $(1) -d test.$(1).write.rio -c $(2) -w $(3)
	@$(J) $(TARGET) -p $(T_VTX) --size $(1) -d test.$(1).write.vtx -c $(2) -w $(3)
endef

bench_dummy: $(TARGET) $(TEST_FILES)
	@echo "=============== Dummy benchmarks =============="
	@echo "Dummy 1M"
	$(call _bench_dummy,1000,1000,100)
	@echo "Dummy 10M"
	$(call _bench_dummy,10000,1000,100)
	@echo "Dummy 100M"
	$(call _bench_dummy,100000,500,50)
	@echo "Dummy 1G"
	$(call _bench_dummy,1000000,100,10)

bench_read: $(TARGET) $(TEST_FILES)
	@echo "=============== Read benchmarks ==============="
	@echo "Read 1M"
	$(call _bench_read,1000,1000,100)
	@echo "Read 10M"
	$(call _bench_read,10000,1000,100)
	@echo "Read 100M"
	$(call _bench_read,100000,500,50)
	@echo "Read 1G"
	$(call _bench_read,1000000,100,10)

bench_write: $(TARGET) $(TEST_FILES)
	@echo "=============== Write benchmarks =============="
	@echo "Write 1M"
	$(call _bench_write,1000,1000,100)
	@echo "Wrte 10M"
	$(call _bench_write,10000,1000,100)
	@echo "Write 100M"
	$(call _bench_write,100000,500,50)
	@echo "Write 1G"
	$(call _bench_write,1000000,100,10)


bench_copy: $(TARGET) $(TEST_FILES)
	@echo "=============== Copy benchmarks ==============="
	@echo "Copy 1M"
	$(call _bench_copy,1000,1000,100)
	@echo "Copy 10M"
	$(call _bench_copy,10000,1000,100)
	@echo "Copy 100M"
	$(call _bench_copy,100000,500,50)
	@echo "Copy 1G"
	$(call _bench_copy,1000000,100,10)

bench_all: bench_dummy bench_read bench_write bench_copy

benchmarks: bench_all

