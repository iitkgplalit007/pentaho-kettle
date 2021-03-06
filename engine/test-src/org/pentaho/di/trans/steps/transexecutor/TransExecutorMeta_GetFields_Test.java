package org.pentaho.di.trans.steps.transexecutor;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.trans.step.StepMeta;

import static org.mockito.Mockito.*;

public class TransExecutorMeta_GetFields_Test {

  private TransExecutorMeta meta;

  private StepMeta executionResult;
  private StepMeta resultFiles;
  private StepMeta outputRows;

  @Before
  public void setUp() {
    executionResult = mock( StepMeta.class );
    resultFiles = mock( StepMeta.class );
    outputRows = mock( StepMeta.class );

    meta = new TransExecutorMeta();
    meta.setExecutionResultTargetStepMeta( executionResult );
    meta.setResultFilesTargetStepMeta( resultFiles );
    meta.setOutputRowsSourceStepMeta( outputRows );

    meta.setExecutionTimeField( "executionTime" );
    meta.setExecutionResultField( "true" );
    meta.setExecutionNrErrorsField( "1" );

    meta.setResultFilesFileNameField( "resultFileName" );

    meta.setOutputRowsField( new String[] { "outputRow" } );
    meta.setOutputRowsType( new int[] { 0 } );
    meta.setOutputRowsLength( new int[] { 0 } );
    meta.setOutputRowsPrecision( new int[] { 0 } );
  }

  @Test
  public void getFieldsForExecutionResults() throws Exception {
    RowMetaInterface mock = invokeGetFieldsWith( executionResult );
    verify( mock, times( 3 ) ).addValueMeta( any( ValueMeta.class ) );
  }

  @Test
  public void getFieldsForResultFiles() throws Exception {
    RowMetaInterface mock = invokeGetFieldsWith( resultFiles );
    verify( mock ).addValueMeta( any( ValueMeta.class ) );
  }

  @Test
  public void getFieldsForInternalTransformationOutputRows() throws Exception {
    RowMetaInterface mock = invokeGetFieldsWith( outputRows );
    verify( mock ).addValueMeta( any( ValueMeta.class ) );
  }

  private RowMetaInterface invokeGetFieldsWith( StepMeta stepMeta ) throws Exception {
    RowMetaInterface rowMeta = mock( RowMetaInterface.class );
    meta.getFields( rowMeta, "", null, stepMeta, null, null, null );
    return rowMeta;
  }
}
