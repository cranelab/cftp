package net.engio.mbassy.dispatch.el;

import java.lang.reflect.Method;
import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

public class StandardELResolutionContext extends ELContext {
  private final ELResolver resolver;
  
  private final FunctionMapper functionMapper;
  
  private final VariableMapper variableMapper;
  
  private final Object message;
  
  public StandardELResolutionContext(Object message) {
    this.message = message;
    this.functionMapper = new NoopFunctionMapper();
    this.variableMapper = new MsgMapper();
    this.resolver = (ELResolver)new BeanELResolver(true);
  }
  
  public ELResolver getELResolver() {
    return this.resolver;
  }
  
  public FunctionMapper getFunctionMapper() {
    return this.functionMapper;
  }
  
  public VariableMapper getVariableMapper() {
    return this.variableMapper;
  }
  
  private class MsgMapper extends VariableMapper {
    private static final String msg = "msg";
    
    private final ValueExpression msgExpression = ElFilter.ELFactory().createValueExpression(StandardELResolutionContext.this.message, StandardELResolutionContext.this.message.getClass());
    
    public ValueExpression resolveVariable(String s) {
      return !s.equals("msg") ? null : this.msgExpression;
    }
    
    public ValueExpression setVariable(String s, ValueExpression valueExpression) {
      return null;
    }
    
    private MsgMapper() {}
  }
  
  private class NoopFunctionMapper extends FunctionMapper {
    private NoopFunctionMapper() {}
    
    public Method resolveFunction(String s, String s1) {
      return null;
    }
  }
}
