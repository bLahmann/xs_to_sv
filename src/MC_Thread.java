import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.distribution.NormalDistribution;

public class MC_Thread extends Thread {

    private static final double AMU_TO_MEV = 931.5;
    private static final double NUM_SIGMA = 5;
    private static final double LIGHT_SPEED = 3e10;

    private PolynomialSplineFunction xsFunction;
    private double beamMass, targetMass;
    private double temperature;
    private int N;

    private NormalDistribution beamDist;
    private NormalDistribution targetDist;

    private double sigmaV;

    public MC_Thread(PolynomialSplineFunction xsFunction, double beamMass, double targetMass, double temperature, int N) {
        this.xsFunction = xsFunction;
        this.beamMass = AMU_TO_MEV * beamMass;
        this.targetMass = AMU_TO_MEV * targetMass;
        this.temperature = 1e-3 * temperature;
        this.N = N;

        beamDist = new NormalDistribution(0.0, Math.sqrt(this.temperature/this.beamMass));
        targetDist = new NormalDistribution(0.0, Math.sqrt(this.temperature/this.targetMass));

    }

    @Override
    public void run() {

        double sum = 0.0;
        for (int i = 0; i < N; i++) {

            double[] vB = new double[]{
                    beamDist.sample(),
                    beamDist.sample(),
                    beamDist.sample()
            };

            double[] vT = new double[]{
                    targetDist.sample(),
                    targetDist.sample(),
                    targetDist.sample()
            };

            // Relative velocity
            double vR = Math.sqrt(Math.pow(vB[0] - vT[0], 2)
                    + Math.pow(vB[1] - vT[1], 2)
                    + Math.pow(vB[2] - vT[2], 2));

            double Eb = 0.5 * beamMass * vR * vR;

            try {
                sum += xsFunction.value(Eb) * vR * LIGHT_SPEED;
            } catch (Exception e) {
                sum += 0.0;
            }

        }

        sigmaV = sum / N;
    }

    public double getSigmaV() {
        return sigmaV;
    }
}
