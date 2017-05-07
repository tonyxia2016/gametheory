package net.funkyjava.gametheory.games.nlhe.javafx;

import static net.funkyjava.gametheory.io.ProgramArguments.getArgument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableLong;

import com.google.common.base.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.cscfrm.CSCFRMNode;
import net.funkyjava.gametheory.extensiveformgame.LinkedActionTreeNode;
import net.funkyjava.gametheory.games.nlhe.javafx.MoveSequenceSelectorViewController.MoveSequenceSelectionListener;
import net.funkyjava.gametheory.games.nlhe.javafx.ThreePlayersPreflopInitStateViewController.State;
import net.funkyjava.gametheory.games.nlhe.javafx.ThreePlayersPreflopInitStateViewController.StateChangeListener;
import net.funkyjava.gametheory.games.nlhe.preflop.HEPreflopHelper;
import net.funkyjava.gametheory.games.nlhe.preflop.ThreePlayersPreflopCSCFRM;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.MoveType;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeNode;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopReducedEquityTable;

@Slf4j
public class ThreePlayersPreflopApp extends Application
    implements StateChangeListener, MoveSequenceSelectionListener {

  private Stage primaryStage;
  private ThreePlayersPreflopViewController ctrl;
  private final String[][] cardsStr = HEPreflopHelper.canonicalPreflopHandNames;
  private static ThreePlayersPreflopReducedEquityTable table;

  private static boolean loadTableFromPath(String[] args) {
    final Optional<String> eqOpt = getArgument(args, ThreePlayersPreflopCSCFRM.equityPathPrefix);
    if (!eqOpt.isPresent()) {
      return false;
    }
    log.info("Loading equity tables from path");
    try {
      final String path = eqOpt.get();
      try (final FileInputStream fis = new FileInputStream(Paths.get(path).toFile())) {
        table = new ThreePlayersPreflopReducedEquityTable();
        table.fill(fis);
        table.expand();
      }
    } catch (Exception e) {
      log.error("Unable to load 3 players preflop equity tables", e);
      return false;
    }
    return true;
  }

  private static boolean loadTableFromResources() {
    log.info("Loading equity tables from resources");
    final URL url = ThreePlayersPreflopApp.class.getResource("3PlayersReduced.dat");
    try (final InputStream stream = url.openStream()) {
      table = new ThreePlayersPreflopReducedEquityTable();
      table.fill(stream);
      table.expand();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static boolean loadTable(String[] args) {
    if (loadTableFromPath(args)) {
      return true;
    }
    return loadTableFromResources();
  }

  @Override
  public void start(Stage primaryStage) {
    this.primaryStage = primaryStage;
    this.primaryStage.setTitle("WT-PF-EQ");
    initRootLayout();
    ctrl.getInitStateViewController().addListener(this);
    ctrl.getSequenceSelectorViewController().addListener(this);
    primaryStage.focusedProperty().addListener((event, old, newVal) -> {
      if (newVal && !old) {
        ctrl.getInitStateViewController().focus();
      }
    });
    handleChange(ctrl.getInitStateViewController().getState());
    ctrl.getComputeMore().onActionProperty().set((event) -> {
      runCSCFRM();
    });
    ctrl.getComputeTime().setText("2");
    ctrl.getComputeTime().focusedProperty().addListener((event, old, newVal) -> {
      if (newVal) {
        Platform.runLater(() -> {
          ctrl.getComputeTime().selectAll();
        });
      }
    });
  }

  public static void main(String[] args) {
    if (!loadTable(args)) {
      System.exit(-1);
      return;
    }
    launch(args);
  }

  public void initRootLayout() {
    try {
      // Load root layout from fxml file.
      FXMLLoader loader = new FXMLLoader();
      loader.setLocation(
          ThreePlayersPreflopApp.class.getResource("view/ThreePlayersPreflopView.fxml"));
      final Parent rootLayout = loader.load();
      ctrl = loader.getController();
      // Show the scene containing the root layout.
      Scene scene = new Scene(rootLayout);
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Optional<ThreePlayersPreflopCSCFRM> cscfrm = Optional.absent();
  private boolean cscfrmRan = false;

  private void updateCSCFRM(final State state) {
    if (cscfrm.isPresent() && cscfrm.get().getRunner().isRunning()) {
      cscfrm.get().getRunner().stop();
    }
    cscfrmRan = false;
    if (!state.isValid()) {
      cscfrm = Optional.absent();
      return;
    }
    final NoBetPlayerData<Integer> sbData = new NoBetPlayerData<>(0, state.getSbStack(), true);
    final NoBetPlayerData<Integer> bbData = new NoBetPlayerData<>(1, state.getBbStack(), true);
    final NoBetPlayerData<Integer> btData = new NoBetPlayerData<>(2, state.getBtStack(), true);
    final List<NoBetPlayerData<Integer>> playersData = new LinkedList<>();
    playersData.add(sbData);
    playersData.add(bbData);
    playersData.add(btData);
    final BetRoundSpec<Integer> betsSpec = new BetRoundSpec<>(0, state.getBbVal());
    final BlindsAnteSpec<Integer> blindsSpecs = new BlindsAnteSpec<>(false, true, false,
        state.getSbVal(), state.getBbVal(), 0, Collections.<Integer>emptyList(), 0, 1);
    final NLHand<Integer> hand = new NLHand<>(playersData, blindsSpecs, betsSpec, 1);
    final ThreePlayersPreflopCSCFRM res = new ThreePlayersPreflopCSCFRM(hand, table, null);
    cscfrm = Optional.of(res);
  }


  @Override
  public void handleChange(State state) {
    selectedSequence = null;
    sequences = null;
    ctrl.getPreflopGridPaneController().clear();
    ctrl.getSequenceSelectorViewController().clearSequences();
    updateCSCFRM(state);
    updateSequences();
    runCSCFRM();

  }

  @Override
  public void selectionChanged(int index, String sequenceString) {
    if (sequences == null || index < 0 || index > this.sequences.size() - 1) {
      return;
    }
    selectedSequence = this.sequences.get(index);
    if (!selectedSequence.getStr().equals(sequenceString)) {
      throw new IllegalStateException(sequenceString + " != " + selectedSequence.getStr());
    }
    updateStrategyGrid();
  }

  private void runCSCFRM() {
    if (!cscfrm.isPresent() || cscfrm.get().getRunner().isRunning()) {
      return;
    }
    final ThreePlayersPreflopCSCFRM cfrm = cscfrm.get();
    ctrl.getComputeMore().disableProperty().set(true);
    final MutableLong computeTime = new MutableLong(2000);
    final String timeStr = ctrl.getComputeTime().getText();
    try {
      final long seconds = Long.parseLong(timeStr);
      if (seconds * 1000l > 0) {
        computeTime.setValue(seconds * 1000l);
      }
    } catch (Exception e) {
    }

    new Thread(() -> {
      cfrm.getRunner().start();
      try {
        synchronized (Thread.currentThread()) {
          Thread.currentThread().wait(computeTime.longValue());
        }
        if (!cfrm.getRunner().isRunning()) {
          return;
        }
        cfrm.getRunner().stopAndAwaitTermination();
        Platform.runLater(() -> onCSCFRMComputationEnd(cfrm));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();
  }

  private void onCSCFRMComputationEnd(final ThreePlayersPreflopCSCFRM cscfrm) {
    if (!this.cscfrm.isPresent() || this.cscfrm.get() != cscfrm) {
      return;
    }
    cscfrmRan = true;
    ctrl.getComputeMore().disableProperty().set(false);
    updateStrategyGrid();
  }

  @Data
  private static class SequenceMove {
    private final LinkedActionTreeNode<NLBetTreeNode<Integer>, ?> node;
    private final int actionIndex;
    private final String str;
  }

  private List<SequenceMove> sequences;
  private SequenceMove selectedSequence;

  private static final Map<Integer, String> playersNames = new HashMap<>();
  static {
    playersNames.put(0, "SB");
    playersNames.put(1, "BB");
    playersNames.put(2, "BT");
  }

  private void updateSequences() {
    if (!cscfrm.isPresent()) {
      return;
    }
    final Map<LinkedActionTreeNode<NLBetTreeNode<Integer>, ?>, CSCFRMNode[]> allNodes =
        cscfrm.get().getData().nodesForEachActionNode();
    sequences = new ArrayList<>();
    allNodes.keySet().forEach(treeNode -> {
      final NLBetTreeNode<Integer> node = treeNode.getPlayerNode().getId();
      final List<Move<Integer>> moves = node.getOrderedMoves();
      for (int i = 0; i < moves.size(); i++) {
        final Move<Integer> move = moves.get(i);
        if (move.getType() == MoveType.FOLD) {
          continue;
        }
        final String str = getString(node.getHand(), move);
        sequences.add(new SequenceMove(treeNode, i, str));
      }
    });
    sequences.sort((seq1, seq2) -> {
      if (seq1.getStr().length() != seq2.getStr().length()) {
        return seq1.getStr().length() - seq2.getStr().length();
      }
      return seq1.getStr().compareTo(seq2.getStr());
    });
    final List<String> movesStrs = new LinkedList<>();
    for (SequenceMove sm : sequences) {
      movesStrs.add(sm.getStr());
    }
    ctrl.getSequenceSelectorViewController().setMoveSequencesStrings(movesStrs);
  }

  private String getString(final NLHand<Integer> hand, Move<Integer> move) {
    String res = "";
    for (List<Move<Integer>> moves : hand.getBetMoves()) {
      for (Move<Integer> m : moves) {
        if (res.length() > 0) {
          res += " - ";
        }
        res += getString(m);
      }
    }
    if (res.length() > 0) {
      res += " - ";
    }
    res += getString(move);
    return res;
  }

  private String getString(final Move<Integer> move) {
    if (move.getType() == MoveType.FOLD) {
      return playersNames.get(move.getPlayerId()) + " FOLD";
    }
    return playersNames.get(move.getPlayerId()) + " PUSH";
  }

  private void updateStrategyGrid() {
    if (!this.cscfrm.isPresent() || !cscfrmRan || selectedSequence == null) {
      ctrl.getPreflopGridPaneController().clear();
      return;
    }
    final CSCFRMNode[] nodes = cscfrm.get().getData().nodesFor(selectedSequence.getNode());
    final double[][] strat = HEPreflopHelper.getMoveStrategy(selectedSequence.getActionIndex(),
        nodes, cscfrm.get().getHoleCardsIndexer());
    ctrl.getPreflopGridPaneController().updateDisplayWithStrategy(strat, cardsStr);
  }
}
