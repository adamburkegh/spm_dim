package org.processmining.plugins.etm.model.narytree.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class CollectData {

	public static void main(String[] args) throws Exception {
		StringTokenizer tok;
		String sep = ";";
		String type = "memory";

		File folder = new File("C://temp//combined//" + type + "//");

		File output = new File("C://temp//combined//combined_" + type + ".csv");
		File output2 = new File("C://temp//combined//combined_pointcloud_" + type + ".csv");

		PrintWriter out = new PrintWriter(output);
		PrintWriter point = new PrintWriter(output2);
		boolean first = true;
		for (File input : folder.listFiles()) {
			String node = input.getName().substring(0, input.getName().indexOf(" "));
			FileInputStream fis = new FileInputStream(input);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
			String line = br.readLine();
			if (first) {
				tok = new StringTokenizer(line, ";");

				String token;
				while (tok.hasMoreTokens()) {
					token = tok.nextToken();
					out.print(token);
					out.print(sep);
				}
				out.print("configured tree size");
				out.print(sep);
				out.print("node");
				out.print(sep);
				out.print("AND");
				out.print(sep);
				out.print("XOR");
				out.print(sep);
				out.print("OR");
				out.print(sep);
				out.print("SEQ");
				out.print(sep);
				out.print("LOOP");
				out.print(sep);
				out.print("TAU");
				out.print(sep);
				out.print("LEAF");
				out.println();

				first = false;
			}
			line = br.readLine();
			line = line.substring(0, line.length() - 1);

			while (line != null) {
				tok = new StringTokenizer(line, ";");

				int i, noise = 0, size = 0;
				double t, nTime = 0.0, ilpTime = 0.0, lpTime = 0.0, cilpTime = 0.0;
				int c = 0;
				String token = null;
				while (tok.hasMoreTokens()) {
					token = tok.nextToken().trim();
					switch (c) {
						case 0 :
							noise = (int) (20.0 * Double.parseDouble(token));
							break;
						case 4 :
							nTime = 1000.0 * Double.parseDouble(token);
							break;
						case 10 :
							ilpTime = 1000.0 * Double.parseDouble(token);
							break;
						case 16 :
							lpTime = 1000.0 * Double.parseDouble(token);
							break;
						case 22 :
							cilpTime = 1000.0 * Double.parseDouble(token);
							break;
						default :
					}
					try {
						i = Integer.parseInt(token);
						out.printf("%5d", i);
					} catch (NumberFormatException e) {
						try {
							t = Double.parseDouble(token);
							out.printf("%18.12f", t);
						} catch (NumberFormatException e2) {
							out.print(token);

						}
					}

					out.print(sep);
					c++;
				}
				// Last token was the tree.
				ProbProcessArrayTree tree = TreeUtils.fromString(token);
				tree = tree.applyConfiguration(0);
				size = tree.size();
				int cnt;
				out.print(tree.size());
				out.print(sep);

				out.print(node);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.AND);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.XOR);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.OR);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.ILV);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.SEQ) + tree.countNodes(ProbProcessArrayTree.REVSEQ);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.LOOP);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				cnt = tree.countNodes(ProbProcessArrayTree.TAU);
				size -= cnt;
				out.print(cnt);
				out.print(sep);

				out.print(size);

				out.println();

				point.println(noise + sep + size + sep + nTime + sep + "0");
				point.println(noise + sep + size + sep + lpTime + sep + "1");
				point.println(noise + sep + size + sep + ilpTime + sep + "2");
				point.println(noise + sep + size + sep + cilpTime + sep + "3");
				//				out.println(line);
				line = br.readLine();
			}
			br.close();
			fis.close();
		}
		out.close();
		point.close();

	}
}
