package org.mitre.synthea.helpers.overrides;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

import org.mitre.synthea.engine.Module;
import org.mitre.synthea.engine.State;
import org.mitre.synthea.engine.State.SetAttribute;
import org.mitre.synthea.engine.Transition;
import org.mitre.synthea.engine.Transition.ComplexTransition;
import org.mitre.synthea.engine.Transition.ComplexTransitionOption;
import org.mitre.synthea.engine.Transition.DistributedTransition;
import org.mitre.synthea.engine.Transition.DistributedTransitionOption;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.helpers.Config;

public class Export {

  public static void main(String[] args) throws IOException {
    Config.set("module_override", ""); // ensure that the overrides are not already set
    
    File outDirectory = Exporter.getOutputFolder(".", null);
    
    List<String> allParams = new LinkedList<>();
    
    for (String moduleName : Module.getModuleNames()) {
      Module module = Module.getModuleByPath(moduleName);
      
      if (module.getClass() != Module.class) {
        continue; // it's one of the java classes
      }
      
      
      for (String stateName : module.getStateNames()) {
        State state = module.getState(stateName);

        String safeStateName = stateName.replace(" ", "\\ "); // spaces need to be escaped in java properties

        String statePath = moduleName + ".json\\:\\:$['states']['" + safeStateName + "']";
        
        if (state instanceof SetAttribute && ((SetAttribute)state).value instanceof Number) {
          // if it's a number we can override it. strings and other things get skipped here
          
          Number value = (Number)((SetAttribute)state).value;
          
          if (value instanceof Integer || value instanceof Long) {
            allParams.add(statePath + "['value'] = " + value.toString());
          } else {
            // not totally safe, but good enough for now
            // assume it's a float
            allParams.add(statePath + "['value'] = " + String.format("%.8f",  value));
          }
        }
        
        Transition t = state.getTransition();
        
        if (t instanceof DistributedTransition) {
          
          processTransitions(allParams, ((DistributedTransition)t).transitions, statePath + "['distributed_transition']");

        } else if (t instanceof ComplexTransition) {
          
          List<ComplexTransitionOption> ctos = ((ComplexTransition)t).transitions;
          for (int i = 0 ; ctos != null && i < ctos.size() ; i++) {
            ComplexTransitionOption cto = ctos.get(i);
            
            
            processTransitions(allParams, cto.distributions, statePath + "['complex_transition'][" + i + "]['distributions']");
          }
        }
      }
      
    }
    System.out.println(allParams.size());
    Path outFilePath = outDirectory.toPath()
        .resolve("overrides" + System.currentTimeMillis() + ".properties");
    
    Files.write(outFilePath, allParams, StandardOpenOption.CREATE_NEW);
    System.out.println("Wrote file " + outFilePath.toAbsolutePath().toString());
  }
  
  private static void processTransitions(List<String> allParams, List<DistributedTransitionOption> distributions, String basePath) {
    for (int i = 0 ; distributions != null && i < distributions.size() ; i++) {
      if (i > 0 && i == distributions.size() - 1) {
        // don't add the last distribution as a parameter, unless there's only 1 in the first place
        // ex. if there are 2 transition options then there aren't really 2 independent parameters, there's only x and the other is implicitly 1-x
        // if there are 3 transition options, there are x, y, and 1-x-y. So just ignore the last distribution here
        continue;
      }
      DistributedTransitionOption dto = distributions.get(i);
      if (dto.distribution instanceof Double) {
        allParams.add(basePath + "[" + i + "]['distribution'] = " + String.format("%.8f",  dto.distribution));
      }
    }
  }
}
