package parser.jarparser;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.generic.*;

/**
 * Disassembler visitor that uses Apache Byte Code Engineering library for disassembling bytecode.
 * This does partial decompilation, as it translates the class structure with declared fields
 * and methods to Java source, but instructions inside methods are translated as commented out
 * JVM Assembly mnemomics.
 */
public class DisassembleVisitor extends EmptyVisitor {

  private final static String NOTICE_TEXT =
          "/**\n" +
          " * Bytecode partially decompiled by CodeCompass.\n" +
          " */\n\n\n";

  // Regular expression patterns to strip package or class names
  private final static String STRIP_PACKAGE_NAMES_REGEXP = ".*(\\.)";
  private final static String MATCH_INNER_CLASS_TYPE_REGEXP = ".*(\\$)";

  private JavaClass currentClass;
  private ConstantPool constantPool;
  private StringBuilder codePrintBuilder;

  public DisassembleVisitor(final JavaClass javaClass) {
    this.codePrintBuilder = new StringBuilder();
    this.currentClass = javaClass;
    this.constantPool = javaClass.getConstantPool();
  }

  public void start() {
    visitJavaClass(currentClass);
  }

  @Override
  public void visitJavaClass(final JavaClass javaClass) {
    codePrintBuilder.append(NOTICE_TEXT);

    // Package
    String packageName = javaClass.getPackageName();
    if (!packageName.isEmpty()) {
      codePrintBuilder.append("package ")
              .append(packageName)
              .append(";\n\n");
    }

    // Class
    if (javaClass.isClass()) {
      codePrintBuilder.append("public class ")
              .append(javaClass.getClassName().replaceAll(STRIP_PACKAGE_NAMES_REGEXP, ""));

      // Superclass name
      if (!javaClass.getSuperclassName().equals("java.lang.Object")) {
        codePrintBuilder.append(" extends ")
                .append(javaClass.getSuperclassName());
      }

      // Interface names
      String[] interfaceNames = javaClass.getInterfaceNames();
      if (0 < interfaceNames.length) {
        codePrintBuilder.append(" implements ");
        for (String interfaceName : interfaceNames) {
          codePrintBuilder.append(interfaceName);
        }
      }
      codePrintBuilder.append(" {\n");

      // Fields
      visitFields(javaClass.getFields());

      // Methods
      visitMethods(javaClass.getMethods());
    } else {
      // Interface
      codePrintBuilder.append("public interface ")
              .append(javaClass.getClassName().replaceAll(STRIP_PACKAGE_NAMES_REGEXP, ""))
              .append(" {\n");

      // Methods
      visitMethods(javaClass.getMethods());
    }
    codePrintBuilder.append("}");
  }

  public void visitFields(final Field[] fields) {
    if (fields != null) {
      if (0 < fields.length) {
        for (Field field : fields) {
          field.accept(this);
        }
        codePrintBuilder.append('\n');
      }
    }
  }

  @Override
  public void visitField(final Field field) {
    if (!field.isSynthetic()) {
      codePrintBuilder.append('\t')
              .append(field.toString())
              .append(";\n");
    }
  }

  public void visitMethods(final Method[] methods) {
    if (methods != null) {
      if (0 < methods.length) {
        for (int i = 0; i < methods.length; ++i) {
          methods[i].accept(this);

          if (i < methods.length - 1) {
            codePrintBuilder.append("\n\n");
          }
        }
        codePrintBuilder.append('\n');
      }
    }
  }

  @Override
  public void visitMethod(final Method method) {
    MethodGen methodG = new MethodGen(method, currentClass.getClassName(), new ConstantPoolGen(constantPool));

    // Is this a constructor?
    if (methodG.getName().equals("<init>")) {
      // Change name "<init>" to name of the class
      String constructorName = currentClass.getClassName().substring(currentClass.getClassName().lastIndexOf('.') + 1);
      methodG.setName(constructorName);

      codePrintBuilder.append("\t");

      // Remove "void" return type from constructor signature
      codePrintBuilder.append(methodG.toString().replaceFirst("void ", ""));
    }
    // Use regular method otherwise
    else {
      codePrintBuilder.append('\t')
              .append(methodG);
    }

    // Check if method is abstract
    if (methodG.isAbstract()) {
      codePrintBuilder.append(";\n");
    } else {
      codePrintBuilder.append(" {\n");
      // method.getCode().accept(this);
      visitInstructions(methodG.getInstructionList());
      codePrintBuilder.append("\t}");
    }
  }

  public void visitInstructions(final InstructionList instructions) {
    InstructionHandle ihandle = instructions.getStart();
    while (ihandle != null) {
      Instruction instruction = ihandle.getInstruction();

      // TODO: cast operators
      String constantPoolReference = "";
      if (instruction instanceof CPInstruction) {
        int poolIndex = ((CPInstruction) instruction).getIndex();
        Constant constant = constantPool.getConstant(poolIndex);
        String constantName = constantPool.constantToString(constant);

        switch (instruction.getOpcode()) {
          case Const.NEWARRAY:   /* fallthrough */  // Primitive array
          case Const.NEW:        /* fallthrough */  // Reference to classes
          case Const.CHECKCAST:  /* fallthrough */
          case Const.INSTANCEOF: /* fallthrough */
          case Const.ANEWARRAY:  /* fallthrough */  // Array containing reference types
          case Const.MULTIANEWARRAY:                // Multidimensional reference array
            constantPoolReference = "<" + constantName + ">";
            break;
          default:
            constantPoolReference = constantName;
        }
      }
      String formattedInstruction = String.format("\t\t// %1$-4d: %2$-20s %3$s\n",
              ihandle.getPosition(),
              instruction.getName(),
              constantPoolReference);
      codePrintBuilder.append(formattedInstruction);

      ihandle = ihandle.getNext();
    }
  }

  public char[] getDecompiledCode() {
    if (codePrintBuilder.toString().isEmpty()) {
      start();
    }

    return codePrintBuilder.toString().toCharArray();
  }
}
