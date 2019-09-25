

JFLAGS = -d
JC = javac

all: compile replace

compile:
	mkdir Distrib && \
	$(JC) $(JFLAGS) Distrib *.java

replace:
	mv ./Distrib/Distrib/* ../../out/production/Project/Distrib
	rm -r Distrib

clean:
	$(RM) ../../out/production/Project/Distrib/*.class