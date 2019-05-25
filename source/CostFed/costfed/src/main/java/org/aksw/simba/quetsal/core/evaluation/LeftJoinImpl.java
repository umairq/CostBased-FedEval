package org.aksw.simba.quetsal.core.evaluation;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;
import com.fluidops.fedx.evaluation.iterator.RestartableLookAheadIteration;
import com.fluidops.fedx.structures.QueryInfo;
import org.aksw.simba.quetsal.core.JoinOrderOptimizer;
import org.aksw.simba.quetsal.core.error.RelativeError;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Set;

public class LeftJoinImpl  extends RestartableLookAheadIteration<BindingSet> {
    public static Logger log = LoggerFactory.getLogger(LeftJoinImpl.class);
    private final EvaluationStrategy strategy;
    private final LeftJoin join;
    private final Set<String> scopeBindingNames;
    private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;
    private volatile CloseableIteration<BindingSet, QueryEvaluationException> rightIter;


    int leftCount = 0;
    int rightCount = 0;

    public LeftJoinImpl(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings)
    {
        this.strategy = strategy;
        this.join = join;
        this.scopeBindingNames = join.getBindingNames();
        this.leftIter = strategy.evaluate(join.getLeftArg(), bindings);
        this.rightIter = new EmptyIteration();


    }

    @Override
    protected BindingSet getNextElement() throws QueryEvaluationException {
        try {
            CloseableIteration nextRightIter = this.rightIter;

            while(nextRightIter.hasNext() || this.leftIter.hasNext()) {
                BindingSet leftBindings = null;
                if (!nextRightIter.hasNext()) {
                    leftBindings = (BindingSet)this.leftIter.next();
                    nextRightIter.close();
                    nextRightIter = this.rightIter = this.strategy.evaluate(this.join.getRightArg(), leftBindings);
                }
                rightCount++;
                while(nextRightIter.hasNext()) {

                    BindingSet rightBindings = (BindingSet)nextRightIter.next();

                    try {
                        if (this.join.getCondition() == null) {
                            return rightBindings;
                        }

                        QueryBindingSet scopeBindings = new QueryBindingSet(rightBindings);
                        scopeBindings.retainAll(this.scopeBindingNames);
                        if (this.strategy.isTrue(this.join.getCondition(), scopeBindings)) {
                            return rightBindings;
                        }
                    } catch (ValueExprEvaluationException var5) {
                        ;
                    }
                }

                if (leftBindings != null) {
                    return leftBindings;
                }
            }
        } catch (NoSuchElementException var6) {
            ;
        } catch (Exception ex){}

        return null;
    }
    protected void handleClose() throws QueryEvaluationException {

            String errText2 = "";
            if (JoinOrderOptimizer.cardJoin.size()>0) {
                errText2 = "\n-------------------\nLeft Join Real Cardinality: " + rightCount +
                        "\nJoin Estimated: " + JoinOrderOptimizer.cardJoin.get(0).nd.card +
                        "\nJoin Relative Error: " + RelativeError.getRelativeError(JoinOrderOptimizer.cardJoin.get(0).nd.card, rightCount) +
                        "\n-------------------\n";
                JoinOrderOptimizer.cardJoin.remove(0);
                try {
                    writeErrorString(errText2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        try {

            super.handleClose();
        }finally {
            try {
                this.leftIter.close();
            } finally {
                this.rightIter.close();
            }
        }


    }
    private void writeErrorString(String result) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("RelativeError.txt", true));
        writer.append(result);
        writer.close();
    }


}
