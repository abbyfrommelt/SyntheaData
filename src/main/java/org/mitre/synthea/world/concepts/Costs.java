package org.mitre.synthea.world.concepts;

import java.util.Random;

import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;

public class Costs {
  

  /**
   * Whether or not this HealthRecord.Entry has an associated cost on a claim.
   * Billing cost is not necessarily reimbursed cost or paid cost.
   * @param entry HealthRecord.Entry
   * @return true if the entry has a cost; false otherwise
   */
  public static boolean hasCost(Entry entry) {
    return (entry instanceof HealthRecord.Procedure)
        || (entry instanceof HealthRecord.Medication)
        || (entry instanceof HealthRecord.Encounter)
        || (entry instanceof HealthRecord.Immunization);
  }

  /**
   * Calculate the cost of this Procedure, Encounter, Medication, etc.
   * 
   * @param entry Entry to calculate cost of.
   * @param patient Person to whom the entry refers to
   * @param provider Provider that performed the service, if any
   * @param payer Entity paying for the service, if any
   * @return Cost, in USD.
   */
  public static double calculateCost(Entry entry, Person patient, Provider provider, String payer) {
      return 0;
    }
    
  /**
   * Helper class to store a grouping of cost data for a single concept.
   * Currently cost data includes a minimum, maximum, and mode (most common value).
   * Selection of individual prices based on this cost data should be done
   * using the chooseCost(Random) method.
   */
  private static class CostData {
    private double min;
    private double mode;
    private double max;
    
    private CostData(double min, double mode, double max) {
      this.min = min;
      this.mode = mode;
      this.max = max;
    }
    
    /**
     * Select an individual cost based on this cost data. Uses a triangular distribution
     * to pick a randomized value.
     * @param random Source of randomness
     * @return Single cost within the range this set of cost data represents
     */
    private double chooseCost(Random random) {
      return triangularDistribution(min, max, mode, random.nextDouble());
    }
    
    /**
     * Pick a single value based on a triangular distribution. 
     * See: https://en.wikipedia.org/wiki/Triangular_distribution
     * @param min Lower limit of the distribution
     * @param max Upper limit of the distribution
     * @param mode Most common value
     * @param rand A random value between 0-1
     * @return a single value from the distribution
     */
    public static double triangularDistribution(double min, double max, double mode, double rand) {
      double f = (mode - min) / (max - min);
      if (rand < f) {
        return min + Math.sqrt(rand * (max - min) * (mode - min));
      } else {
        return max - Math.sqrt((1 - rand) * (max - min) * (max - mode));
      }
    }
  }
}
