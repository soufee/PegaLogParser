import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {


    public static void main(String[] args) throws IOException, InterruptedException {
        String SOURCE = System.getProperty("user.dir");
        List<String> list = new ArrayList<>();
        String files[] = new File(SOURCE).list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].toString().endsWith("log"))
                list.add(files[i]);
        }

      //  System.out.println(list);
        Thread[] threads = new Thread[list.size()];
        for (int i = 0; i < list.size(); i++) {
            threads[i] = new MyRunnable(list.get(i), args[0]);
          //  threads[i] = new MyRunnable(list.get(i), "RnrcSystemUser");
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++)
            threads[i].join();

        //     Set<String> contracts = getListOfWrongHash(SOURCE);
        //     Map<String, String> detailedContracts = getDifferenceOfHashWrongContracts(SOURCE, contracts);
        //    analize(detailedContracts);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите exit для выхода");
        while(true){
            String s = scanner.nextLine();
            if (s.equals("exit")) break;
        }
    }


    static class MyRunnable extends Thread {
        private String SOURCE;
        private String USER_NAME;
        public MyRunnable(String SOURCE, String USER_NAME) {
            this.SOURCE = SOURCE;
            this.USER_NAME = USER_NAME;
        }

        public void run() {
            System.out.println("Запускается обработка файла " + SOURCE + " для поиска записей от " + USER_NAME);
            Set<String> contracts = getListOfWrongHash(SOURCE);
            Map<String, String> detailedContracts = getDifferenceOfHashWrongContracts(SOURCE, contracts);
            analize(detailedContracts);
        }

        private  void analize(Map<String, String> contracts) {
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, String> entry : contracts.entrySet()) {
                list.add(entry.getKey());
                String s = entry.getValue();
                String[] data = s.split(";");
                if (data[0].equals("")) {
                    list.add("В договоре нет допсов");
                } else {
                    list.add("Допсы по договору: " + data[0]);

                }
                try {
                    String[] la = data[1].split(" ");
                    String[] bi = data[2].split(" ");
                    list.add("Количество убытков: " + la[2]);
                    list.add("Количество бордеро и счетов: " + bi[2]);
                    if (data[3].trim().equals(data[4].trim())) {
                        int sectionsNum = Integer.parseInt(data[3].trim());
                        list.add("Количество секций в диасофте и пеге совпадает: " + sectionsNum);
                        List<String> pega = new ArrayList<>();
                        List<String> diasoft = new ArrayList<>();

                        for (int i = 5; i < 5 + sectionsNum; i++) {
                            list.add("Секция " + (i - 4) + " в Пега: " + data[i].trim());
                            pega.add(data[i].trim());
                        }
                        for (int i = 5 + sectionsNum; i < 5 + sectionsNum * 2; i++) {
                            list.add("Секция " + (i - 4 - sectionsNum) + " в Диасофт: " + data[i].trim());
                            diasoft.add(data[i].trim());
                        }

                        for (int i = 0; i < pega.size(); i++) {
                            if (!diasoft.contains(pega.get(i))) {
                                list.add("Не совпадение секции " + (i + 1));
                            }
                        }
                    } else {
                        list.add("Количество секций не совпадает: В пеге " + data[3].trim() + ", в диасофт " + data[4].trim());
                    }
                } catch (Exception e) {
                    list.add("Ошибка обработки строки " + s);
                }
            }

            try (FileOutputStream stream = new FileOutputStream("report.txt");
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {
                for (String s : list) {
                    System.out.println(s);
                    writer.write(s + "\n");
                }
                writer.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println(list.size()+" размер листа, который выводится в report.txt");
        }

        private  Map<String, String> getDifferenceOfHashWrongContracts(String source, Set<String> contractIDs) {
            Set<String> set = new HashSet<>();
            Map<String, String> results = new HashMap<>();

            try (FileInputStream fstream = new FileInputStream(source);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"))) {
                String pattern = "(.*) "+USER_NAME+" - (.*)";
                System.out.println("pattern "+pattern);
                Pattern r = Pattern.compile(pattern);
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    Matcher m = r.matcher(strLine);
                    if (m.find()) {
                        int start = strLine.indexOf(USER_NAME) + 17;
                        int end = strLine.length();
                        //  System.out.println(strLine.substring(start, end));
                        set.add(strLine.substring(start, end));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String contract : contractIDs) {
                String pattern1 = contract + "(.*)";
                String pattern2 = "(.*) Не найдено соответствие секции по хэшу";
                String pattern3 = "(.*)совпадение(.*)";
                String pattern4 = "(.*)платежей(.*)";
                Pattern r = Pattern.compile(pattern1);
                Pattern r2 = Pattern.compile(pattern2);
                Pattern r3 = Pattern.compile(pattern3);
                Pattern r4 = Pattern.compile(pattern4);
                for (String s : set) {
                    Matcher m = r.matcher(s);
                    Matcher m2 = r2.matcher(s);
                    Matcher m3 = r3.matcher(s);
                    Matcher m4 = r4.matcher(s);
                    if (m.find()) {
                        if (!m2.find() && !m3.find() && !m4.find()) {
                            String[] q = s.split(":");
                           results.put(q[0].trim(), q[1].trim());
                        }
                    }
                }
            }
            try (FileOutputStream stream = new FileOutputStream("differences.txt");
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"))) {
                for (String s : set) {
                    System.out.println(s);
                    writer.write(s + "\n");
                }
                writer.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return results;
        }

        private  Set<String> getListOfWrongHash(String fileName) {
            Set<String> set = new HashSet<>();
            try (FileInputStream fstream = new FileInputStream(fileName);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fstream, "UTF-8"))) {
                String pattern = "(.*): Не найдено соответствие секции по хэшу";
                Pattern r = Pattern.compile(pattern);
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    Matcher m = r.matcher(strLine);
                    if (m.find()) {
                        int start = strLine.indexOf(USER_NAME) + 17;
                        int end = strLine.indexOf(": Не найдено");
                        //    System.out.println(strLine.substring(start, end) + ", ");
                        set.add(strLine.substring(start, end));
                    }
                }

             } catch (IOException e) {
                System.out.println("Ошибка");
            }
            try(FileOutputStream stream = new FileOutputStream("HashNotMatchSA.txt");
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"))){
                for (String s : set) {
                    System.out.println(s);
                    writer.write(s + "\n");
                }
                writer.flush();
            }catch (IOException e) {
                System.out.println("Ошибка2");
            }

            return set;
        }


    }
}
