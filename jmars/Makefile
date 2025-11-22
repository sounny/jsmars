#############################################################################
##
## CONTROL SECTION FOR EACH JMARS EDITION 
##

# There's a group of control variables for each jmars jar file:
#
#     DEFINES is a list of the #define'd variables for the config file.
#     LIBS    is a list of the lib/blah.zip files needed.
#     EXCLUDE is a list of wildcards to exclude from that jar file.

## Libraries needed by all distributions
COMMON_LIBS= \
	BrowserLauncher2-1_3.jar \
	commons-beanutils-1.7.0.jar \
        commons-codec-1.9.jar \
	commons-collections-2.1.1.jar \
	commons-fileupload-1.2.2.jar \
	commons-io-2.1.jar \
	commons-logging-1.2.jar \
	cookformlayout-1.1.jar \
	cookswing-1.5.jar \
	cookxml-3.0.1.jar \
	dom4j-1.6.1.jar \
	forms-1.1.0.jar \
	hsqldb-1.8.0_10.jar \
	javacsv.jar \
	jaxen-1.1-beta-6.jar \
	jcommon-1.0.10.jar \
	jfreechart-1.0.16.jar \
	mysql-connector-java-5.1.5-bin.jar \
	openmap-4.6.5.jar \
	postgresql-8.0-311.jdbc2.jar  \
	stampserver.jar \
	swingx-1.0.jar \
	trove.zip \
	xstream-1.4.2.jar \
	xmlpull-1.1.3.1.jar \
	xpp3_min-1.1.4c.jar \
	backport-util-concurrent-3.0.jar \
	ehcache-2.7.1.jar \
	slf4j-api-1.6.6.jar \
	slf4j-jdk14-1.6.6.jar \
	jsr107cache-1.0.jar \
	jaxb-impl.jar \
	jsr173_1.0_api.jar \
	JavaAPIforKml.jar \
	jaxb-api.jar \
	activation.jar \
	jts-1.11.jar \
	gt-opengis-2.7.1.jar \
	gt-api-2.7.1.jar \
	gt-main-2.7.1.jar \
	gt-metadata-2.7.1.jar \
	gt-referencing-2.7.1.jar \
	gt-shapefile-2.7.1.jar \
	gt-epsg-wkt-2.7.1.jar \
	gt-data-2.7.1.jar \
	jsr-275-1.0-beta-2.jar \
	vecmath-1.3.2.jar \
	jai_imageio-1.1.jar \
	jai_codec-1.1.3.jar \
	jai_core-1.1.3.jar \
	je-4.0.103.jar \
	jel-2.0.1.jar \
	json.jar \
	httpclient-4.5.1.jar \
	httpclient-cache-4.5.1.jar \
	httpcore-4.4.4.jar \
	httpmime-4.5.1.jar \
	ws-commons-util-1.0.2.jar \
	xmlrpc-client-3.1.3.jar \
	xmlrpc-common-3.1.3.jar \
	xmlrpc-server-3.1.3.jar \
	2013_signalpro_mars_spectra_vis.jar \
	ini4j-0.5.2-SNAPSHOT.jar \
	material-ui-swing-1.1.3-rc2.jar \
	jiconfont-1.0.1.jar \
	jiconfont-swing-1.0.2.jar \
	commons-lang3-3.9.jar \
	commons-text-1.6.jar \
	guava-11.0.2.jar \
	gson-1.4.jar \
	javassist-3.16.1-GA.jar \
	jsr305-1.3.9.jar \
	reflections-0.9.9-RC1-uberjar.jar \
	batik-all-1.11.jar \
	fop-transcoder-allinone-2.3.jar \
	js.jar \
	lombok-1.18.8.jar \
	serializer-2.7.2.jar \
	xalan-2.7.2.jar \
	xercesImpl-2.9.1.jar \
	xml-apis-1.3.04.jar \
	xml-apis-ext-1.3.04.jar \
	xmlgraphics-commons-2.3.jar \
	jdom-1.1.3.jar \
	jsoup-1.12.1.jar \
	rome-1.0.jar \
	swingstates-1.0.0.jar \
	commons-cli-1.4.jar \
	JTextFieldPlaceholder-0.0.2-rc1-SNAPSHOT.jar \
	SwingSnackBar-0.0.2.jar	\
	balloontip-1.2.4.jar \
	swing-autocomplete-1.0.4.jar \
	rsyntaxtextarea-3.3.3.jar \
	commons-validator-1.7.jar \
	badlogic.jar 
	
public-jmars.jar:	DEFINES = PUBLIC_MARS
public-jmars.jar:	LIBS = $(COMMON_LIBS)
		
#############################################################################

# The path separator for java classpaths
S := :

TMPDIR := .build-tmp
CLSDIR := .class-tmp

PACKAGES_CMD := find . -name '*.java' | perl -ne 's|./(.+)/[^/]+$$|$$1\n|;y|/|.|;$$p{$$_}++||print' | grep -v '\.\.'
DOCCLASSES := find edu/asu/jmars -type d | grep -v '\/\.' | sed 's/\//\./g'
#find edu/asu/jmars -type d -not -name CVS | perl -e '<"$_/*.java">&&y{/}{.},print'
#find -name '*.java' | perl -ne 's|./(.+)/[^/]+$|$1\n|;y|/|.|;$p{$_}++||print'


LIBRARIES = $(wildcard lib/*.zip)

# Programs (with common options):
SHELL		= /bin/sh
RM              = rm -f
MV              = mv -f
SED		= sed
ETAGS		= etags
XARGS		= xargs
CAT		= cat
FIND            = find
CPP		= cpp -C -P

INSTALL         = install
INSTALL_PROG    = $(INSTALL) -m $(MODE_PROGS)
INSTALL_FILE    = $(INSTALL) -m $(MODE_FILES)
INSTALL_DIR     = $(INSTALL) -m $(MODE_DIRS) -d

# Install modes
MODE_PROGS      = 555
MODE_FILES      = 444
MODE_DIRS       = 2755

# Build programs
JAVAC           = javac
JAVADOC         = javadoc
JAR             = jar

# Build flags
ALL_LIB_ZIPS    = $(sort $(wildcard lib/*.zip lib/*.jar util/i4jruntime.jar))
CLASSPATH       = $(subst $S ,,$(subst $S ,$S,$(addsuffix $S,$(ALL_LIB_ZIPS))) )

JAVAC_FLAGS     = -g -deprecation -d $(CLSDIR) -J-Xms256m  -J-Xmx2G 
JAVADOC_FLAGS   = -link http://java.sun.com/j2se/1.5/docs/api/ -use -package
JAR_FLAGS       = cfm
JIKES_DEP_FLAG	= +M

# Perl one-liner to find outdated java files that need recompiling
CMD_FIND_JAVAS = perl -e 'print join" ",grep{chop,/\.java/;(stat)[9]>((stat"$(CLSDIR)/$$`.class")[9]||0)}`find edu/asu/jmars net/signalpro -name "*.java"`'

JAVA_FILES = $(shell $(CMD_FIND_JAVAS))
CLASS_FILES = $(addprefix $(CLSDIR)/,$(JAVA_FILES:.java=.class))

# ------------------------------------------------------------------- #

# Prefix for every install directory
PREFIX		=

# Where to start installing the class files. Set this to an empty value
#  if you dont want to install classes
CLASS_DIR	= $(PREFIX)classes

# The directory to install the jar file in. Set this to an empty value
#  if you dont want to install a jar file
JAR_DIR	        = $(PREFIX)lib

# The directory to install html files generated by javadoc
DOC_DIR         = $(PREFIX)docs

# The directory to install script files in
SCRIPT_DIR	= $(PREFIX)bin

# ------------------------------------------------------------------- #

# Everything
ALL_JAR_FILES = \
	public-jmars.jar \

# Everything the jar files depend on
JAR_FILES_DEPS = $(patsubst %.java,%.class,$(wildcard *.java \
					resources/* \
					images/* \
					$(LIBRARIES) \
					Makefile )) \
					$(CLASS_FILES)

JMARS_CONFIG = resources/jmars.config
JMARS_USAGE = resources/usage

# ------------------------------------------------------------------- #


# Packages we should compile
PACKAGES = student

# Resource packages
RESOURCES =


# Directories with shell scripts
SCRIPTS =

# ------------------------------------------------------------------- #

# A marker variable for the top level directory
TOPLEVEL	:= .

# Subdirectories with java files:
JAVA_DIRS	:= $(subst .,/,$(PACKAGES)) $(TOPLEVEL)

# Subdirectories with only resource files:
RESOURCE_DIRS	:= $(subst .,/,$(RESOURCES))

# All the .xjava source files:
XJAVA_SRC	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.xjava))

# All the xjava files to build
XJAVA_OBJS	:= $(XJAVA_SRC:.xjava=.java)

# All the .java source files:
JAVA_SRC	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.java))
JAVA_SRC	:= $(XJAVA_OBJS) $(JAVA_SRC)

# Dependency files:
DEPEND_OBJS	:= $(JAVA_SRC:.java=.u)

# Objects that should go into the jar file. (find syntax)
JAR_OBJS	:= \( -name '*.class' -o -name '*.au' \
		       -o -name '*.properties' -o -name '*.cert' \)

# The intermediate java files and main classes we should build:
JAVA_OBJS	:= $(XJAVA_OBJS) $(JAVA_SRC:.java=.class)

# Resource files:
#  Extend the list to install other files of your choice
RESOURCE_SRC	:= *.properties *.gif *.au
#  Search for resource files in both JAVA_DIRS and RESOURCE_DIRS
RESOURCE_OBJS	:= $(foreach dir, $(JAVA_DIRS) $(RESOURCE_DIRS), \
		     $(wildcard $(foreach file, $(RESOURCE_SRC), \
		     $(dir)/$(file))))

# All the shell scripts source
SCRIPT_SRCS 	:= $(foreach dir, $(SCRIPTS), $(wildcard $(dir)/*.sh))
# All shell scripts we should install
SCRIPT_OBJS    	:= $(SCRIPT_SRCS:.sh=)

# All the files to install into CLASS_DIR
INSTALL_OBJS	:= $(foreach dir, $(JAVA_DIRS), $(wildcard $(dir)/*.class))
# Escape inner class delimiter $
INSTALL_OBJS	:= $(subst $$,\$$,$(INSTALL_OBJS))
# Add the resource files to be installed as well
INSTALL_OBJS	:= $(INSTALL_OBJS) $(RESOURCE_OBJS)


# ------------------------------------------------------------------- #

define check-exit
|| exit 1

endef

.SUFFIXES: .java .class .u .xjava .jar .zip

# -----------
# Build Rules
# -----------

RETRO = java -cp /themis/lib/retroguard.jar:$(CLSDIR)

# -------
# Targets
# -------

.PHONY: all jar install uninstall doc clean depend tags
.SILENT: 
 
CMD_FIND_PACKS := -name \*.java|perl -ne'(($$x)=/^package (.+);/)&&last for(`cat $$_`);$$x=~y-.-/-;/^$$x\//||print"PACKAGE NAMING PROBLEM IN: $$_"'

find_package_problems:
	@find edu $(CMD_FIND_PACKS)

src: edu
	ln -s edu/asu/jmars src

#%.class: %.java
$(CLASS_FILES): $(JAVA_FILES)
	mkdir -p $(CLSDIR)
	$(CMD_FIND_JAVAS) | xargs $(JAVAC) $(JAVAC_FLAGS) -classpath '$(CLASSPATH)$S.'

define build-about
	date > resources/about.txt
	date '+%s' >> resources/about.txt
	find edu -name '*.java' | xargs cat | wc -l >> resources/about.txt
	find edu -name '*.java' | wc -l >> resources/about.txt
	find $(CLSDIR) -name '*.class' | wc -l >> resources/about.txt
endef

help:
	@echo "Usage: make {all|jar|install|uninstall|docs|clean|depend|tags}"

jar:  public-jmars.jar
jars: $(ALL_JAR_FILES)


reverse = $(shell perl -e 'print join" ",reverse @ARGV' $(1))


# Used to create the $(TMPDIR), specifically not implemented as a dependency.
define build-TMPDIR
	@echo
	echo '=== Creating build directory ($(TMPDIR))...'
	echo 
	echo '	-> Removing previous build'
	rm -rf $(TMPDIR)
	mkdir -p $(TMPDIR)
	echo '	-> Adding in-house class/resource files'
	cp -r resources images $(CLSDIR)/edu $(TMPDIR)
	perl -ne 'if(/^\s*config\.url\.(\S+)\s+(.+)/){print STDERR "\t-> Appending $$1 config file\n";print"## FROM $$1 $$2\n",`wget -q -O - "$$2?jmars_config=1"`}' < $(TMPDIR)/$(JMARS_CONFIG) >> $(TMPDIR)/$(JMARS_CONFIG)
	cd $(TMPDIR); $(foreach x,$(call reverse,$(LIBS)),echo '	-> Adding library: $(x)';$(JAR) xf ../lib/$(x);)
	echo '	-> Trimming the fat'
	find $(TMPDIR) -mindepth 1 -name '*.java' -o -name CVS -o -name '.*' -o -name '*~' -o -name 'LICENSE*' -o -name 'license*' | xargs rm -rf
	rm -rf $(addprefix $(TMPDIR)/,$(EXCLUDE))
endef

# rule to build jar distributions
$(ALL_JAR_FILES): $(LIBRARIES) $(JAR_FILES_DEPS)
	@echo 
	@echo '========== Making $@ =========='
	@echo
	@echo '=== Building about.txt file...'
	$(build-about)
	$(build-TMPDIR)
	@echo
	@echo '=== Building the jar file...'
	@echo 
	$(JAR) cmf manifest $@ -C $(TMPDIR) .

# Doc target <-- auto-created by mmake, slightly modified manually on 6/21/01
#ifneq ($(strip $(PACKAGES)),)
docs:	docs/index.html

docs/index.html: Makefile $(shell find edu/asu/jmars -name "*.java")
	@echo "===> [Generating documentation, entry point is $(DOC_DIR)/index.html] "
	$(INSTALL_DIR) $(DOC_DIR) $(check-exit)
	$(DOCCLASSES) | xargs $(JAVADOC) -windowtitle "JMARS javadocs" -doctitle "JMARS" -classpath ".$S$(CLSDIR)$S$(CLASSPATH)" -d $(DOC_DIR) $(JAVADOC_FLAGS)
	@echo "===> [Documentation generated, entry point is $(DOC_DIR)/index.html] "

# Various cleanup routines
clean:
	rm -rf *.class $(CLSDIR) $(TMPDIR)
	rm -R *.* edu/asu/jmars/test
