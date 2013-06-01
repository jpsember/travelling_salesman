package salesman;

import myUtils.*;
import Genetic.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class Salesman
    extends FrameApplet
    implements Globals {
  private SalesmanPanel panel;

  public void openComponents(Container c) {
    panel = new SalesmanPanel(c);
  }

  public void stop() {
    panel.stop();
    //panel.thread = null;
  }

  /*    implements Globals {
    private SalesmanPanel p;
    public void init() {
      setLayout(new BorderLayout());
      p = new SalesmanPanel(this);
    }
   */
  /* public void destroy() {
     p.destroy();
   }
   */
}

class SalesmanPanel
    extends TestBed
    implements IGenEvaluator, GenGlobals {

  private static final int
      S_START = 2
      , S_RUNNING = 3
//      , SITES = 30
      , POP_SIZE = 50
      , MAP_X = 1000
      , MAP_Y = 600
      ;

  private static final int
      B_RESET = 0
      , B_RUN = 1
      , B_STEP = 2
      , B_STOP = 3
      , B_SEED = 4
      , B_DEFAULT = 5
      ;

  private static final String[] buttonNames = {
      ".M RandoM)ize",
      ".R R)un",
      ".S S)tep",
      ".X (X)Stop",
      ".C C)hange cities",
      ".D D)efault order",
  };
  public final int[] list0 = {
      B_STEP,
      B_RUN,
      B_STOP,
      B_SEED,
      B_RESET,
      B_DEFAULT,
  };

  private PDisp pDisp;

  private eThread thread;

  public SalesmanPanel(Container c) {
    super("Travelling Salesman");

    initItems(1965);
    initPop();

    thread = new eThread(this);

    Panel p = new Panel(new BorderLayout());

    p.add(this, "Center");
    pDisp = new PDisp(pop);
    p.add(pDisp, "West");

    construct(c, p, this, buttonNames, list0);
  }

  public void prepared() {
    setView(0, MAP_Y, MAP_X, 0, 4);
//    setScale(MAP_X,-MAP_Y,4);
    if (state == 0) {
      setState(S_START);
      setState(S_RUNNING);
    }
  }

  public void stop() {
    thread.cmd.setArgLock(0, thread.DIE);
  }

  /**
     Set the state.
     Repaints the window.
     @param s :     new state (S_xxx)
   */
  private void setState(int s) {

    boolean refresh = (state != s);

    state = s;

    switch (state) {

      case S_START:
        Fmt.setNumberFormat(8, 2);
        break;
      case S_RUNNING:
        initPop();
        break;

    }
    if (refresh) {
      updateDisplay();
    }
    repaintButtons();
  }

  private int state;

  public void paintInterior(Graphics g) {
    super.paintInterior(g);

    if (state != S_START) {
      Genotype t = pop.getGenotype(0);
      eval(t);
      displayBest();
    }

  }

  private void displayBest() {
    /*
        Point drawSize = getInteriorSize();
        final int INSET = 4;
        double PWIDTH = Math.min( (drawSize.x - INSET * 2) / (double) MAP_X,
                                 (drawSize.y - INSET * 2) / (double) MAP_Y);
        Point origin = centerRect( (int) (PWIDTH * MAP_X + INSET),
                                  (int) (PWIDTH * MAP_Y) + INSET);
     */
//   setScale(MAP_X,MAP_Y,INSET);
//    setOrigin(origin);
//    this.setScale(PWIDTH);
    setColor(Color.black);
    Point prev = (Point) sites.get(order[sites.size() - 1]);
    for (int i = 0; i < sites.size(); i++) {
      Point pt = (Point) sites.get(order[i]);
      drawLine(pt.x, pt.y, prev.x, prev.y);
      prev = pt;
    }
    setColor(Color.blue);
    for (int i = 0; i < sites.size(); i++) {
      Point pt = (Point) sites.get(order[i]);
      final Point dest = new Point();
      this.worldToPanel(pt.x, pt.y, dest);
      this.graphics().fillOval(dest.x - 2, dest.y - 2, 4, 4);
    }
//    this.clearScale();
  }

  /**
   * Enable the buttons according to the state
   */
  public int disabledButtons() {
    int disabled = 0;

    int c = thread.cmd.getArgLock(0);
    if (c == eThread.RUN)
      disabled |= (1 << B_RUN) | (1 << B_STEP);
    else
      disabled |= (1 << B_STOP);

    return disabled;
  }

  public void updateDisplay() {
    repaint();
    pDisp.repaint();
    setPrompt(pop.getStats());
  }

  /**
   * Process a button press.
   * @param b button pressed
   */
  public void processButtonPress(int button) {
    switch (button) {
      case B_STOP:
        thread.cmd.setArgLock(0, thread.STOP);
        break;
      case B_RUN:
        process(false);
        break;
      case B_STEP:
        process(true);
        break;
      case B_DEFAULT:
        pop.clearGenomes(false);
        updateDisplay();
        break;
      case B_RESET:
        thread.cmd.setArgLock(0, thread.STOP);
        setState(S_RUNNING);
        updateDisplay();
        break;

      case B_SEED: {
        thread.cmd.setArgLock(0, thread.STOP);
        initItems(math.rnd(3000));
        initPop();
        pop.clearHistory();
        setState(S_RUNNING);
        updateDisplay();
      }
      break;
    }
  }

  private int resetCounter;

  private void initItems(int seed) {
    boolean circle = (resetCounter++ & 3) == 0;
    if (circle)
      seed = math.rnd(2000);

    Random r = new Random(seed);

    sites.clear();
    int numSites = math.rnd(50) + 15;
    if (resetCounter % 5 == 0)
      numSites = 300;

    for (int i = 0; i < numSites; i++) {
      int x = tools.rnd(r, MAP_X);
      int y = tools.rnd(r, MAP_Y);
      if (circle) {
        double ang = r.nextFloat() * Math.PI * 2.0;
        x = (int) (Math.cos(ang) * (MAP_X * .48)) + MAP_X / 2;
        y = (int) (Math.sin(ang) * (MAP_Y * .48)) + MAP_Y / 2;
      }

      sites.add(new Point(x, y));
    }
    pop = null;
  }

  private DArray sites = new DArray();
  protected Population pop;

  private void initPop() {
    if (pop == null) {
      pop = new Population(this, POP_SIZE, OPT_MINIMIZE
                           | OPT_ELITE
                           );
      pop.addTourField(sites.size());
      pop.complete();
      if (pDisp != null)
        pDisp.setPop(pop);
    }
    pop.init();
    if (false) {
      if (sites.size() < 100)
        pop.setRestartInterval(250);
      else
        pop.setRestartInterval(0);
    }
    pop.setOperProb(.25, .005);

    pop.clearGenomes(true);
//    pop.display();
  }

  private void process(boolean step) {
    if (state == S_START) {
      setState(S_RUNNING);
      return;
    }

    thread.cmd.setArgLock(0, step ? thread.STEP : thread.RUN);
  }

  private int[] order; // = new int[SITES];
//  private boolean[] orientations = new boolean[SITES];

  public void extractValues(Genotype g) {
    if (order == null || order.length < sites.size())
      order = new int[sites.size()];
    for (int i = 0; i < sites.size(); i++) {
      order[i] = pop.getSite(g, 0, i);
    }
  }

  public double eval(Genotype g) {
    extractValues(g);

    double dist = 0.0;
    Point prev = (Point) sites.get(order[sites.size() - 1]);

    for (int i = 0; i < sites.size(); i++) {
      Point pt = (Point) sites.get(order[i]);
      dist += math.distance(pt, prev);
      prev = pt;
    }
    return dist / sites.size();
  }

  public String description(Genotype g) {
    return "ROUTE";
  }

  public void evolve() {
    pop.evolve();
  }
}

class eThread
    implements Runnable {

  public static final int
      RUN = 1
      , STOP = 2
      , STEP = 3
      , DIE = 99
      ;

  public ThreadCommand cmd = new ThreadCommand(2);

  private SalesmanPanel panel;

  public eThread(SalesmanPanel panel) {
    this.panel = panel;
    Thread t = new Thread(this);
    t.setDaemon(true);
    t.start();

  }

  // Runnable interface
  public void run() {
    int currCmd = 0;
    long prevTicks = System.currentTimeMillis();
    while (currCmd != DIE) {
      boolean update = false;

      synchronized (cmd) {
        currCmd = cmd.getArg(0);
        switch (currCmd) {
          case RUN:
            panel.evolve();
            update = (System.currentTimeMillis() - prevTicks > 150);
            break;
          case STEP:
            panel.evolve();
            update = true;
            cmd.setArg(0, 0);
            break;
        }
      }
      if (update) {
        prevTicks = System.currentTimeMillis();
        panel.updateDisplay();
      }
      if (currCmd != RUN)
        cmd.sleep(100);
    }
  }
  /*
   private void buildCities() {
      final double R=63781.5;
      final double S = 0.00000001745329251994; // (converts millionths of a degree to radians)
      for (int i = 0; i < cities.length; i+=2) {
    R*acos(
    cos(lon[x]*S)*cos(lat[x]*S)*cos(lon[y]*S)*cos(lat[y]*S)+
    sin(lon[x]*S)*sin(lat[x]*S)*sin(lon[y]*S)*sin(lat[y]*S)+
    sin(lat[x]*S)*sin(lat[y]*S)
    )
    where
    acos() is the arccosine function,
   }
    private final int[] cities = {
        86799223, 33527746,
        86627723, 34707048,
        88088959, 30677450,
        86284287, 32354400,
        112201590, 33584128,
        111740337, 33417736,
        112071399, 33542550,
        111870478, 33685992,
        111930639, 33388350,
        110891717, 32195816,
        92354076, 34722400,
        117872331, 33838900,
        119004553, 35357700,
        122297290, 37867250,
        117044733, 32628450,
        121286669, 38691650,
        121999870, 37973250,
        118168562, 34032650,
        118027797, 34074800
        ,117071270, 33136445
        ,121997122, 37528506
        ,119792874, 36780600
        ,117928054, 33884800
        ,117959375, 33778750
        ,118253257, 34176720
        ,122104602, 37627667
        ,118008115, 33691755
        ,118343649, 33955850
        ,117795807, 33662114
        ,118159824, 33788900
        ,118411201, 34112101
        ,120993923, 37659799
        ,117210311, 33926230
        ,122224550, 37771544
        ,117310286, 33225074
        ,117605810, 34054412
        ,117823999, 33805250
        ,119213695, 34197426
        ,118138724, 34160700
        ,117761399, 34058950
        ,117570239, 34123831
        ,117397363, 33940437
        ,121467360, 38566850
        ,121635081, 36684833
        ,117292299, 34139750
        ,117135770, 32814950
        ,122554783, 37793250
        ,121849783, 37304000
        ,117882114, 33736400
        ,118510175, 34413408
        ,122700799, 38448600
        ,118751299, 34263050
        ,121306741, 37969850
        ,122025531, 37385750
        ,118867557, 34192700
        ,118340424, 33834800
        ,122264186, 38107467
        ,104729772, 39712267
        ,104759899, 38863200
        ,104872655, 39768035
        ,105113556, 39695200
   ,      73196209 , 41186316
   ,      72683866 , 41765700
   ,      72924068 , 41310100
   ,      73552688 , 41096733
   ,      73037429 , 41558400
   ,      77016167 , 38905050
   ,      80139499 , 26142550
   ,      80296772 , 25861750
   ,      80164762 , 26029817
   ,      81657692 , 30334550
   ,      80210845 , 25775667
   ,      81374248 , 28504748
   ,      82642121 , 27758000
   ,      84281399 , 30457000
   ,      82482120 , 27959000
   ,      84422592 , 33762900
   ,      84874972 , 32510710
   ,      83656839 , 32836750
   ,      81131648 , 32024357
        ,116226100, 43606651
   ,     87684965 , 41837050
   ,      89609225 , 40744964
   ,      89063149 , 42270300
   ,      89644654 , 39781433
       ,87543299 , 37984150
       ,85139024 , 41073750
       ,87344149 , 41595750
       ,86146196 , 39776400
       ,86265699 , 41675300
       ,91669578 , 41972967
       ,93617405 , 41576738
       ,94727037 , 39118400
       ,94684916 , 38914592
       ,95691999 , 39037900
       ,97342674 , 37687350
       ,84459460 ,38042746
       ,85741156 ,38224750
       ,91126043 ,30448967
       ,90177396 ,29997601
       ,89931355 ,30065846
       ,93797808 ,32471550
       , 76610616 ,39300800
   ,      71017892 ,42336029
   ,      71322141 ,42638711
   ,      70938117 ,41661300
   ,      72539045 ,42115033
   ,      71808920 ,42269633
       ,83730841 ,42275350
       ,83102198 ,42383100
       ,83692799 ,43022850
       ,85655727 ,42961250
       ,84553996 ,42709100
       ,83372524 ,42398150
       ,83030534,42580035
       ,83028197,42493000
       ,93266849,44961850
       ,93103686,44947744
       ,90207591,32320500
       ,94353413,39089344
       ,94552009,39122312
       ,90244299,38636050
       ,93286099,37196200
       ,96688171,40816400
       ,96011745,41263900
        ,115222799,36205750
        ,115133602,36080825
        ,119822499,39538700
       , 74194157,40666367
   ,      74064962,40711300
   ,      74173245,40724100
   ,      74163413,40914450
        ,106624636,35117218
   ,     73799017,42665750
   ,      78859684,42889800
   ,      73943849,40669800
   ,      77615838,43168651
   ,      76144067,43041059
   ,      73867514,40947033
       ,80834514,35197550
   ,     78914969,35980433
   ,      79826888,36078900
   ,      78658753,35821950
       ,80262910,36102100
       ,81521499,41080400
       ,84505957,39139801
       ,81678511,41479700
       ,82987381,39988933
       ,84197357,39779067
       ,83581649,41663950
       ,97513491,35467050
       ,95916407,36127750
        ,123112172,44053000
        ,122656496,45538250
        ,123022057,44924500
   ,      75477665,40596333
       ,80086596,42125850
      ,  75134678,40006817
   ,      79976702,40439207
   ,      71419732,41821950
       ,96730100,43544201
       ,85256956,35066209
       ,83946288,35974550
       ,90006991,35105600
       ,86784829,36171550
       ,99738973,32454500
        ,101818682,35202550
       ,97127510,32694501
       ,97750522,30305880
       ,94144463,30087767
       ,97292827,27705728
       ,96765249,32794151
        ,106437549,31849250
       ,97336249,32753901
       ,96629300,32910800
       ,97016653,32692239
       ,95386728,29768700
       ,96969338,32857685
       ,99486599,27534300
        ,101875374,33575850
       ,96599800,32769001
       ,95152021,29660600
       ,96746664,33046201
       ,98505355,29457650
       ,97183198,31568950
        ,111929921,40777267
      ,  77086178,38821000
   ,      77101909,38878726
   ,      76308771,36678812
   ,      76291910,37055150
   ,      76514160,37075850
   ,      76244943,36923200
   ,      76356410,36855534
   ,      77474584,37531050
   ,      76043668,36739356
        ,122350326,47621800
        ,117414024,47672300
        ,122459766,47252000
       ,89387519,43079800
       ,87966623,43063350
    };
   */
}