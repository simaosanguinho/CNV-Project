package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class BranchStatistics extends AbstractJavassistTool {

    /**
     * Maps method long names to a map of basic block positions to the number of executions.
     */
    private static Map<Long, Map<String, Map<Integer, Integer>>> counters = new HashMap<>();

    public BranchStatistics(List<String> packageNameList, String writeDestination) {
        super(packageNameList,writeDestination);
    }

    public static void visitBasicBlock(String methodLongName, int position) {

        if (!counters.containsKey(Thread.currentThread().getId())) {
            counters.put(Thread.currentThread().getId(), new HashMap<>());
        }

        if (!counters.get(Thread.currentThread().getId()).containsKey(methodLongName)) {
            counters.get(Thread.currentThread().getId()).put(methodLongName, new HashMap<>());
        }

        if (!counters.get(Thread.currentThread().getId()).get(methodLongName).containsKey(position)) {
            counters.get(Thread.currentThread().getId()).get(methodLongName).put(position, 0);
        }

        counters.get(Thread.currentThread().getId()).get(methodLongName).put(position, counters.get(Thread.currentThread().getId()).get(methodLongName).get(position) + 1);
    }


    public static void printStatistics() {
        System.out.println("HELLO FROM " + BranchStatistics.class.getSimpleName() + "!");
        for (Map.Entry<String, Map<Integer, Integer>> method : counters.get(Thread.currentThread().getId()).entrySet()) {
            for (Map.Entry<Integer, Integer> basicblock : method.getValue().entrySet()) {
                System.out.println(String.format("[%s] %s basic block %s was called %s times", BranchStatistics.class.getSimpleName(), method.getKey(), basicblock.getKey(), basicblock.getValue()));
            }
        }
    }


    public static String checkStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s] Statistics for thread %s:\n", BranchStatistics.class.getSimpleName(), Thread.currentThread().getId()));
        for (Map.Entry<String, Map<Integer, Integer>> method : counters.get(Thread.currentThread().getId()).entrySet()) {
            for (Map.Entry<Integer, Integer> basicblock : method.getValue().entrySet()) {
                sb.append(String.format("[%s] %s basic block %s was called %s times\n", BranchStatistics.class.getSimpleName(), method.getKey(), basicblock.getKey(), basicblock.getValue()));
            }
        }
        return sb.toString();
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", BranchStatistics.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        for (int incoming : block.entrances) {
            System.out.println(String.format("[%s] Basic block %s has an outgoing edge to basic block %s",
                    BranchStatistics.class.getSimpleName(), incoming, block.getPosition()));
        }


        block.behavior.insertAt(block.line, String.format("%s.visitBasicBlock(\"%s\", %s);", BranchStatistics.class.getName(), block.getBehavior().getLongName(), block.getPosition()));
    }
}