import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {


    private static final int N_CPUs = 45;
    private static final int N = (int) 2e6;


    private static final File file = new File("/home/lahmann/xs_to_sv/data/DTn_ENDF.dat");
    private static final double beamMass = 2;
    private static final double targetMass = 3;

    public static void main(String ... args) throws Exception{

        PolynomialSplineFunction xsFunction = parseXS(file);
        FileWriter w = new FileWriter(new File(file.getAbsolutePath().replace(".dat", ".sv")), true);

        double[] temperatures = logspace(0, 3, 201);
        for (double T : temperatures) {

            MC_Thread[] threads = new MC_Thread[N_CPUs];
            for (int i = 0; i < N_CPUs; i++) {
                threads[i] = new MC_Thread(xsFunction, beamMass, targetMass, T, N);
                threads[i].start();
            }

            double average = 0;
            for (int i = 0; i < N_CPUs; i++) {
                threads[i].join();
                average += threads[i].getSigmaV();
            }

            String string = String.format("%.8e, %.8e\n", T, average / N_CPUs);
            w.write(string);
            System.out.print(string);

        }

        w.close();
    }


    private static PolynomialSplineFunction parseXS(File file) throws IOException {

        ArrayList<Double> energy   = new ArrayList<>();
        ArrayList<Double> xSection = new ArrayList<>();

        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()){

            String line = scanner.nextLine();
            if (line.contains("#")) continue;

            String[] temp = line.split("\\s+");

            if (Double.valueOf(temp[2]) > 0) {
                energy.add(1e-6 * Double.valueOf(temp[1]));
                xSection.add(1e-24 * Double.valueOf(temp[2]));
            }
        }
        scanner.close();

        double[] E  = new double[energy.size()];
        double[] xs = new double[xSection.size()];
        for (int i = 0; i < E.length; i++){
            E[i]  = energy.get(i);
            xs[i] = xSection.get(i);
        }

        return new LinearInterpolator().interpolate(E, xs);
    }

    private static double[] logspace(double a, double b, int N){
        double[] array = new double[N];
        for (int i = 0; i < N; i++){
            double p = i*(b-a)/(N-1) + a;
            array[i] = Math.pow(10, p);
        }
        return array;
    }
}
