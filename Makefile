.PHONY: clean compile test jar uberjar repl

all: jar install

clean:
	clj -T:build clean

jar:
	clj -T:build jar

install:
	clj -T:build install
