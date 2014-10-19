/**
 * Created by Яна on 15.10.2014.
 */

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Tree<T> {
    private Tree(Node<T> root) {
        this.root = Objects.requireNonNull(root);
    }

    private static class Node<T> {
        T info;
        List<Node<T>> children;

        Node(T info, List<Node<T>> children) {
            this.info = info;
            this.children = children;
        }

        Node(T info, Node<T>... children) {
            this.info = info;
            this.children = new ArrayList<>();
            for (Node<T> child : children) {
                this.children.add(Objects.requireNonNull(child));
            }
        }

        /*
        просматривает дерево в поиске равных значений, являющихся
        непосредственным потомком одного и того же узла. Если такая
        пара значений находится, то соответствующие узлы “склеиваются”,
        то есть один из этих узлов уничтожается, а его потомки переносятся
        к потомкам другого узла
        */

        private void joinEquals() {
            HashMap<T, List<Node<T>>> tempChildren = new HashMap<>();
            for (Node<T> child : children) {
                if (!tempChildren.containsKey(child.info)) {
                    tempChildren.put(child.info, new ArrayList<>());
                }

                tempChildren.get(child.info).addAll(child.children);
            }
            children.clear();

            children.addAll(tempChildren.keySet().stream().map(child -> new Node<T>(child, tempChildren.get(child))).collect(Collectors.toList()));

            for (Node<T> child : children) {
                child.joinEquals();
            }
        }

        /*
       Метод выдает длину максимальной последовательности узлов, в которой
       каждый следующий узел является непосредственным потомком предыдущего,
       и при этом каждый узел кроме последнего в последовательности имеет
       не более одного потомка
        */
        private int longestBranch(int depth) {
            if (children.size() == 0) {
                return depth + 1;
            }

            if (children.size() == 1) {
                return children.get(0).longestBranch(depth + 1);
            }

            int maxDepth = (depth > 0) ? depth + 1 : 0;
            for (Node<T> child : children) {
                int childDepth = child.longestBranch(0);
                maxDepth = (maxDepth < childDepth) ? childDepth : maxDepth;
            }

            return maxDepth;
        }

        /*
       Метод выдают минимальный уровень,
       на котором может находиться терминальный узел дерева
       */
        private int minTermLevel(int depth) {
            if (children.size() == 0) {
                return depth;
            }

            int minDepth = Integer.MAX_VALUE;
            for (Node<T> child : children) {
                int childDepth = child.minTermLevel(depth + 1);
                minDepth = (minDepth > childDepth) ? childDepth : minDepth;
            }

            return minDepth;
        }

        /*
        Метод выдает максимальный уровень, на котором
        может находиться терминальный узел дерева.
         */
        private int maxTermLevel(int depth) {
            if (children.size() == 0) {
                return depth;
            }

            int maxDepth = depth;
            for (Node<T> child : children) {
                int childDepth = child.maxTermLevel(depth + 1);
                maxDepth = (maxDepth < childDepth) ? childDepth : maxDepth;
            }

            return maxDepth;
        }
    }

    private Node<T> root;

    public void joinEquals() {
        root.joinEquals();
    }

    public int minTermLevel() {
        return root.minTermLevel(0);
    }

    public int maxTermLevel() {
        return root.maxTermLevel(0);
    }

    public int longestBranch() {
        return root.longestBranch(0);
    }

    /*
    Вспомогательная функция для анализа дерева в виде скобочной последовательности.
    Предполагается, что запись изначально корректна.
     */
    public static <T> Tree<T> build(Function<String, T> parser, String src) {
        Scanner scanner = new Scanner(src.replaceAll("[(]", " ( ").replaceAll("[)]", " ) "));
        Node<T> root = build(parser, scanner);
        Tree<T> t = new Tree<>(root);
        return t;
    }

    private static <T> Node<T> build(Function<String, T> parser, Scanner scanner) {
        if (!scanner.hasNext()) throw new NoSuchElementException();
        String first = scanner.next();
        if (")".equals(first)) return null;
        Node<T> node = new Node<T>(parser.apply(first));
        if (!"(".equals(scanner.next())) throw new IllegalArgumentException();
        for (; ; ) {
            Node<T> child = build(parser, scanner);
            if (child == null) break;
            node.children.add(child);
            if (")".equals(scanner.next())) break;
        }
        return node;
    }

    public static void main(String[] args) {
        // Проверим нахождение наибольшей "голой" ветки дерева.
        Tree<String> t1 = build(s -> s, "Анна(Борис(Василий(), Виктор()), Татьяна(Алексей(), Михаил(), Сергей()), Ольга(Настасья()))");
        System.out.println(t1.longestBranch());
        System.out.println(t1);

        // Проверим слияние  узлов. Таковым является 0.
        Tree<Integer> t2 = build(Integer::parseInt, "2(5(1(), 10()), 7(12(), 0(), -6(), 0(1(), 2(), 3())), 6(23()))");
        t2.joinEquals();
        System.out.println(t2);

        // Проверим макс и мин уровни.
        Tree<String> t3 = build(s -> s, "Животные(Простейшие(), Губки(), Кишечнополостные(), Черви(), " +
                "Моллюски(), Членистоногие(Ракообразные(), Паукообразные(), Насекомые()), Хордовые(Рыбы(), " +
                "Земноводные(), Пресмыкающиеся(), Птицы(), Млекопитающие(Приматы(), Хоботные(), Парнокопытные(), Китообразные(), " +
                "Хищные(Кошачьи(), Псовые(Собака(), Лисица()), Медвежьи()), Грызуны(Заячьи(), Беличьи(), Мышиные()), Рукокрылые(), " +
                "Насекомоядные())))");
        System.out.println(t3.maxTermLevel());
        System.out.println(t3.minTermLevel());
    }

    private static <T> String toString(Node<T> node) {
        StringBuilder builder = new StringBuilder(node.info.toString());
        builder.append("(");
        boolean hasChild = false;
        for (Node<T> child : node.children) {
            if (hasChild) builder.append(", ");
            hasChild = true;
            builder.append(toString(child));
        }
        builder.append(")");
        return builder.toString();
    }

    public String toString() {
        return toString(root);
    }
}
