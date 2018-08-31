default: all

start:
	lein run -m ahubu.core

test:
	lein test

clean:
	rm -fr dist
	lein clean

shipit: dist.tar.gz

dist.tar.gz: dist
	tar czvf dist.tar.gz dist

dist:
	mkdir dist
	lein deps
	lein uberjar
	cp target/uberjar/ahubu-0.1.0-SNAPSHOT-standalone.jar dist/
	cp -R dist-skel/* dist/
	cp -R conf dist/
	cp -R resources dist/
	cp -R js-src dist/
	cp -R docs dist/
	cp ahubu.png dist/

.PHONY: clean shipit
