package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static Map<Long, Long> nblocks = new HashMap<>();

    /**
     * Number of executed methods.
     */
    private static Map<Long, Long> nmethods = new HashMap<>();

    /**
     * Number of executed instructions.
     */
    private static Map<Long, Long> ninsts = new HashMap<>();

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    /**
     * Returns the number of executed basic blocks by thread
     * and resets counter for next requests
     */
    public static long getNblocks() {
        long blocks = nblocks.get(Thread.currentThread().getId());
        nblocks.replace(Thread.currentThread().getId(), 0L);
        return blocks;
    }

    /**
     * Returns the number of executed methods by thread
     * and resets counter for next requests
     */
    public static long getNmethods() {
        long methods = nmethods.get(Thread.currentThread().getId());
        nmethods.replace(Thread.currentThread().getId(), 0L);
        return methods;
    }

    /**
     * Returns the number of executed instructions by thread
     * and resets counter for next requests
     */
    public static long getNinsts() {
        long insts = ninsts.get(Thread.currentThread().getId());
        ninsts.replace(Thread.currentThread().getId(), 0L);
        return insts;
    }

    public static void incBasicBlock(int position, int length) {
        Long key = Thread.currentThread().getId();
        nblocks.putIfAbsent(key, 0L);
        nblocks.replace(key, nblocks.get(key) + 1);
        ninsts.putIfAbsent(key, 0L);
        ninsts.replace(key, ninsts.get(key) + length);
    }

    public static void incBehavior(String name) {
        Long key = Thread.currentThread().getId();
        nmethods.putIfAbsent(key, 0L);
        nmethods.replace(key, nmethods.get(key) + 1);
    }

    public static void printStatistics() {
        Long threadId = Thread.currentThread().getId();
        System.out.println(String.format("[%s] Thread: %s Number of executed methods: %s", ICount.class.getSimpleName(), Thread.currentThread().getId(), nmethods.get(threadId)));
        System.out.println(String.format("[%s] Thread: %s Number of executed basic blocks: %s", ICount.class.getSimpleName(), nblocks.get(threadId)));
        System.out.println(String.format("[%s] Thread: %s Number of executed instructions: %s", ICount.class.getSimpleName(), ninsts.get(threadId)));
    }

    /**
     *  Shows the metrics corresponding to the application
     *  metrics in String format to store on a file
     *  // TODO maybe use json later
     * @return a string with statistics
     */
    public static String checkStatistics() {
        long threadId = Thread.currentThread().getId();
        long ninsts = getNinsts();
        long nmethods = getNmethods();
        long nblocks = getNblocks();
        return "Thread: " + Thread.currentThread().getId() + ",\n"
            + "nblocks: " + nblocks + ",\n"
            + "nmethods: " + nmethods + ",\n"
            + "ninsts: " + ninsts + "\n";
    }

    // return the number of instructions executed in the current thread
    public static long checkNinsts() {
        return getNinsts();
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICount.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", ICount.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
