JAVAPARSERBASEDIR=parser/javaparser-java

JAVAPARSER_SRC=\
	$(JAVAPARSERBASEDIR)/src/parser/database/ConnStringParser.java \
	$(JAVAPARSERBASEDIR)/src/parser/database/DatabasePlatform.java \
	$(JAVAPARSERBASEDIR)/src/parser/database/EMCreator.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/BuildAction.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/BuildLog.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/BuildParameter.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/BuildSource.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/BuildTarget.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/File.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/FileContent.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaAnnotation.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaAstNode.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaDocComment.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaEnumConstant.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaFunction.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaImport.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaMember.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaType.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/JavaVariable.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/Option.java \
	$(JAVAPARSERBASEDIR)/src/parser/entity/Project.java \
	$(JAVAPARSERBASEDIR)/src/parser/exception/ParseException.java \
	$(JAVAPARSERBASEDIR)/src/parser/jarparser/DecompileVisitor.java \
	$(JAVAPARSERBASEDIR)/src/parser/jarparser/JarParser.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/ArgParser.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/AstVisitor.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/Frame.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/NameExtractor.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/NameMangler.java \
	$(JAVAPARSERBASEDIR)/src/parser/sourceparser/PositionInfo.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/Common.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/Hashing.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/HtmlBuilder.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/LogConfigurator.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/ProblemHandler.java \
	$(JAVAPARSERBASEDIR)/src/parser/util/Query.java \
	$(JAVAPARSERBASEDIR)/src/parser/JavaParserServiceImpl.java \
	$(JAVAPARSERBASEDIR)/src/parser/Parser.java

lib/java/javaparser.jar: $(JAVAPARSER_SRC) $(JAVA_PARSER_API_JAVA_SRC)
	mkdir -p $(JAVAPARSERBASEDIR)/bin
	$(JAVAC) $(JAVAC_FLAGS) -cp $(JAVA_BUILD_CP):$(JAVAPARSERBASEDIR)/bin:$(JAVAPARSERBASEDIR)/src \
	  -d $(JAVAPARSERBASEDIR)/bin $(JAVAPARSER_SRC) $(JAVA_PARSER_API_JAVA_SRC)
	jar cfmv lib/java/javaparser.jar \
	     $(JAVAPARSERBASEDIR)/src/META-INF/MANIFEST.MF \
	  -C $(JAVAPARSERBASEDIR)/src META-INF/persistence.xml \
	  -C $(JAVAPARSERBASEDIR)/src META-INF/services/java.sql.Driver \
	  -C $(JAVAPARSERBASEDIR)/bin parser \
	  -C $(JAVAPARSERBASEDIR)/bin cc

javalib_DATA+=lib/java/javaparser.jar

CLEANFILES+=\
	lib/java/javaparser.jar

clean-local::
	-rm -rf $(JAVAPARSERBASEDIR)/bin/parser

