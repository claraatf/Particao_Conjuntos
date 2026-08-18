package br.edu.taal.particao.ui;

import br.edu.taal.particao.Main;
import br.edu.taal.particao.experiment.DashboardGenerator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Interface grafica opcional para configurar e acompanhar as baterias.
 *
 * <p>A janela nao possui uma implementacao paralela dos experimentos: ela
 * chama {@link Main#executarExperimentos(String[], PrintStream)} em uma thread
 * de trabalho e apenas apresenta o progresso produzido pelo mesmo fluxo da
 * linha de comando.</p>
 */
public final class ExperimentGui {

    private static final Color COR_CABECALHO = new Color(20, 35, 58);
    private static final Color COR_PRIMARIA = new Color(37, 99, 235);
    private static final Color COR_SUCESSO = new Color(22, 101, 52);
    private static final Color COR_FUNDO_LOG = new Color(15, 23, 42);
    private static final Color COR_TEXTO_LOG = new Color(226, 232, 240);

    private final JFrame janela = new JFrame("Partição de Conjuntos — Experimentos TAAL");
    private final JComboBox<ModoInterface> campoModo = new JComboBox<>(ModoInterface.values());
    private final JTextField campoSeed = new JTextField("42", 16);
    private final JTextField campoSaida = new JTextField(40);
    private final JLabel descricaoModo = new JLabel();
    private final JTextArea areaLog = new JTextArea();
    private final JLabel rotuloStatus = new JLabel("Pronto para executar.");
    private final JProgressBar progresso = new JProgressBar();
    private final JButton botaoProcurar = new JButton("Escolher...");
    private final JButton botaoExecutar = new JButton("Executar bateria");
    private final JButton botaoDashboard = new JButton("Abrir dashboard");
    private final JButton botaoPasta = new JButton("Abrir pasta");

    private Path caminhoAutomatico;
    private Path ultimoCsv;
    private Path ultimoDashboard;

    private ExperimentGui() {
        configurarJanela();
        configurarEventos();
        caminhoAutomatico = caminhoPadrao(ModoInterface.RAPIDO);
        campoSaida.setText(caminhoAutomatico.toString());
        atualizarDescricaoModo();
    }

    /** Abre a janela na thread de eventos do Swing. */
    public static void abrir() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "A interface grafica exige um ambiente com monitor. Use o modo por terminal neste ambiente.");
        }

        aplicarAparenciaDoSistema();
        SwingUtilities.invokeLater(() -> {
            ExperimentGui interfaceGrafica = new ExperimentGui();
            interfaceGrafica.janela.setVisible(true);
        });
    }

    private static void aplicarAparenciaDoSistema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // O tema padrao do Swing continua plenamente funcional.
        }
    }

    private void configurarJanela() {
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setMinimumSize(new Dimension(780, 600));
        janela.setSize(920, 700);
        janela.setLocationByPlatform(true);
        janela.setLayout(new BorderLayout());
        janela.add(criarCabecalho(), BorderLayout.NORTH);
        janela.add(criarConteudo(), BorderLayout.CENTER);
        janela.setLocationRelativeTo(null);
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(0, 6));
        cabecalho.setBackground(COR_CABECALHO);
        cabecalho.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titulo = new JLabel("Partição de Conjuntos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel(
                "Análise comparativa de Backtracking, Branch and Bound, Programação Dinâmica e heurísticas");
        subtitulo.setForeground(new Color(203, 213, 225));

        cabecalho.add(titulo, BorderLayout.NORTH);
        cabecalho.add(subtitulo, BorderLayout.CENTER);
        return cabecalho;
    }

    private JPanel criarConteudo() {
        JPanel conteudo = new JPanel(new BorderLayout(0, 14));
        conteudo.setBorder(new EmptyBorder(16, 20, 16, 20));
        conteudo.add(criarPainelConfiguracao(), BorderLayout.NORTH);
        conteudo.add(criarPainelLog(), BorderLayout.CENTER);
        conteudo.add(criarRodape(), BorderLayout.SOUTH);
        return conteudo;
    }

    private JPanel criarPainelConfiguracao() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Configuração da bateria"),
                new EmptyBorder(6, 8, 8, 8)));

        GridBagConstraints restricoes = new GridBagConstraints();
        restricoes.insets = new Insets(5, 5, 5, 5);
        restricoes.anchor = GridBagConstraints.WEST;

        restricoes.gridx = 0;
        restricoes.gridy = 0;
        painel.add(new JLabel("Modo:"), restricoes);

        restricoes.gridx = 1;
        restricoes.weightx = 1;
        restricoes.fill = GridBagConstraints.HORIZONTAL;
        painel.add(campoModo, restricoes);

        restricoes.gridx = 2;
        restricoes.weightx = 0;
        restricoes.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Seed:"), restricoes);

        restricoes.gridx = 3;
        restricoes.fill = GridBagConstraints.HORIZONTAL;
        painel.add(campoSeed, restricoes);

        restricoes.gridx = 0;
        restricoes.gridy = 1;
        restricoes.gridwidth = 4;
        restricoes.weightx = 1;
        restricoes.fill = GridBagConstraints.HORIZONTAL;
        descricaoModo.setForeground(new Color(71, 85, 105));
        painel.add(descricaoModo, restricoes);

        restricoes.gridx = 0;
        restricoes.gridy = 2;
        restricoes.gridwidth = 1;
        restricoes.weightx = 0;
        restricoes.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("CSV de saída:"), restricoes);

        restricoes.gridx = 1;
        restricoes.gridwidth = 2;
        restricoes.weightx = 1;
        restricoes.fill = GridBagConstraints.HORIZONTAL;
        painel.add(campoSaida, restricoes);

        restricoes.gridx = 3;
        restricoes.gridwidth = 1;
        restricoes.weightx = 0;
        restricoes.fill = GridBagConstraints.NONE;
        painel.add(botaoProcurar, restricoes);

        campoModo.setToolTipText("Escolha entre a verificação rápida, a bateria completa e a escalabilidade.");
        campoSeed.setToolTipText("A mesma seed reproduz as mesmas instâncias.");
        campoSaida.setToolTipText("O dashboard HTML será criado ao lado deste CSV.");
        return painel;
    }

    private JPanel criarPainelLog() {
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        JLabel titulo = new JLabel("Progresso e resumo");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));

        areaLog.setEditable(false);
        areaLog.setLineWrap(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(COR_FUNDO_LOG);
        areaLog.setForeground(COR_TEXTO_LOG);
        areaLog.setCaretColor(Color.WHITE);
        areaLog.setMargin(new Insets(10, 10, 10, 10));
        areaLog.setText("Configure a bateria e clique em \"Executar bateria\".\n");
        DefaultCaret caret = (DefaultCaret) areaLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane rolagem = new JScrollPane(areaLog);
        rolagem.setPreferredSize(new Dimension(760, 320));
        painel.add(titulo, BorderLayout.NORTH);
        painel.add(rolagem, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new BorderLayout(12, 8));

        JPanel estado = new JPanel(new BorderLayout(8, 3));
        progresso.setIndeterminate(false);
        progresso.setVisible(false);
        estado.add(rotuloStatus, BorderLayout.NORTH);
        estado.add(progresso, BorderLayout.CENTER);

        JPanel acoes = new JPanel();
        estilizarBotaoPrimario(botaoExecutar);
        botaoDashboard.setEnabled(false);
        botaoPasta.setEnabled(false);
        acoes.add(botaoPasta);
        acoes.add(botaoDashboard);
        acoes.add(botaoExecutar);

        rodape.add(estado, BorderLayout.CENTER);
        rodape.add(acoes, BorderLayout.EAST);
        return rodape;
    }

    private void estilizarBotaoPrimario(JButton botao) {
        botao.setBackground(COR_PRIMARIA);
        botao.setForeground(Color.WHITE);
        botao.setFont(botao.getFont().deriveFont(Font.BOLD));
        botao.setFocusPainted(false);
        botao.setBorder(new EmptyBorder(9, 16, 9, 16));
    }

    private void configurarEventos() {
        campoModo.addActionListener(evento -> atualizarModoSelecionado());
        botaoProcurar.addActionListener(evento -> escolherArquivo());
        botaoExecutar.addActionListener(evento -> iniciarExecucao());
        botaoDashboard.addActionListener(evento -> abrirArquivo(ultimoDashboard, true));
        botaoPasta.addActionListener(evento -> abrirPastaResultado());
    }

    private void atualizarModoSelecionado() {
        ModoInterface modo = modoSelecionado();
        Path novoAutomatico = caminhoPadrao(modo);
        String atual = campoSaida.getText().trim();
        if (atual.isEmpty() || (caminhoAutomatico != null && atual.equals(caminhoAutomatico.toString()))) {
            campoSaida.setText(novoAutomatico.toString());
        }
        caminhoAutomatico = novoAutomatico;
        atualizarDescricaoModo();
    }

    private void atualizarDescricaoModo() {
        descricaoModo.setText("<html>" + modoSelecionado().descricao + "</html>");
    }

    private ModoInterface modoSelecionado() {
        ModoInterface modo = (ModoInterface) campoModo.getSelectedItem();
        return modo == null ? ModoInterface.RAPIDO : modo;
    }

    private void escolherArquivo() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Escolher arquivo CSV de saída");
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivo CSV (*.csv)", "csv"));
        try {
            Path atual = Paths.get(campoSaida.getText().trim()).toAbsolutePath().normalize();
            seletor.setSelectedFile(atual.toFile());
        } catch (InvalidPathException ignorada) {
            seletor.setSelectedFile(caminhoPadrao(modoSelecionado()).toFile());
        }

        if (seletor.showSaveDialog(janela) == JFileChooser.APPROVE_OPTION) {
            Path selecionado = garantirExtensaoCsv(seletor.getSelectedFile().toPath());
            campoSaida.setText(selecionado.toAbsolutePath().normalize().toString());
        }
    }

    private void iniciarExecucao() {
        String[] argumentos;
        try {
            argumentos = criarArgumentos(modoSelecionado(), campoSeed.getText(), campoSaida.getText());
        } catch (IllegalArgumentException excecao) {
            JOptionPane.showMessageDialog(janela, excecao.getMessage(),
                    "Configuração inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ultimoCsv = Paths.get(argumentos[1]).toAbsolutePath().normalize();
        ultimoDashboard = DashboardGenerator.caminhoDashboard(ultimoCsv);
        if (Files.exists(ultimoCsv)) {
            int resposta = JOptionPane.showConfirmDialog(janela,
                    "O CSV selecionado já existe e será substituído ao final da execução.\n"
                            + ultimoCsv + "\n\nDeseja continuar?",
                    "Confirmar substituição", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (resposta != JOptionPane.YES_OPTION) {
                return;
            }
        }
        campoSaida.setText(ultimoCsv.toString());
        areaLog.setText("");
        definirExecutando(true);

        SwingWorker<Void, String> tarefa = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (PrintStream log = new PrintStream(
                        new LineOutputStream(this::publish), true, StandardCharsets.UTF_8)) {
                    Main.executarExperimentos(argumentos, log);
                }
                return null;
            }

            @Override
            protected void process(List<String> trechos) {
                for (String trecho : trechos) {
                    areaLog.append(trecho);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    concluirComSucesso();
                } catch (InterruptedException excecao) {
                    Thread.currentThread().interrupt();
                    concluirComErro("A execução foi interrompida.", excecao);
                } catch (ExecutionException excecao) {
                    Throwable causa = excecao.getCause() == null ? excecao : excecao.getCause();
                    concluirComErro(mensagemErro(causa), causa);
                }
            }
        };
        tarefa.execute();
    }

    private void definirExecutando(boolean executando) {
        campoModo.setEnabled(!executando);
        campoSeed.setEnabled(!executando);
        campoSaida.setEnabled(!executando);
        botaoProcurar.setEnabled(!executando);
        botaoExecutar.setEnabled(!executando);
        botaoDashboard.setEnabled(!executando && ultimoDashboard != null && Files.isRegularFile(ultimoDashboard));
        botaoPasta.setEnabled(!executando && ultimoCsv != null
                && ultimoCsv.getParent() != null && Files.isDirectory(ultimoCsv.getParent()));
        progresso.setVisible(executando);
        progresso.setIndeterminate(executando);
        rotuloStatus.setForeground(executando ? COR_PRIMARIA : UIManager.getColor("Label.foreground"));
        rotuloStatus.setText(executando
                ? "Executando em segundo plano — a janela continuará responsiva."
                : "Pronto para executar.");
    }

    private void concluirComSucesso() {
        definirExecutando(false);
        botaoDashboard.setEnabled(Files.isRegularFile(ultimoDashboard));
        botaoPasta.setEnabled(ultimoCsv.getParent() != null && Files.isDirectory(ultimoCsv.getParent()));
        rotuloStatus.setForeground(COR_SUCESSO);
        rotuloStatus.setText("Bateria concluída. CSV e dashboard foram gerados com sucesso.");
    }

    private void concluirComErro(String mensagem, Throwable causa) {
        definirExecutando(false);
        areaLog.append("\nERRO: " + mensagem + "\n");
        rotuloStatus.setForeground(new Color(185, 28, 28));
        rotuloStatus.setText("A execução não foi concluída.");
        JOptionPane.showMessageDialog(janela, mensagem,
                "Falha na execução", JOptionPane.ERROR_MESSAGE);
        causa.printStackTrace();
    }

    private void abrirPastaResultado() {
        if (ultimoCsv == null || ultimoCsv.getParent() == null) {
            return;
        }
        abrirArquivo(ultimoCsv.getParent(), false);
    }

    private void abrirArquivo(Path caminho, boolean usarNavegador) {
        if (caminho == null || !Files.exists(caminho)) {
            JOptionPane.showMessageDialog(janela, "O arquivo ainda não foi gerado.",
                    "Arquivo indisponível", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            mostrarCaminhoSemSuporte(caminho);
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (usarNavegador && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(caminho.toUri());
            } else if (!usarNavegador && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(caminho.toFile());
            } else {
                mostrarCaminhoSemSuporte(caminho);
            }
        } catch (IOException | SecurityException excecao) {
            JOptionPane.showMessageDialog(janela,
                    "Não foi possível abrir automaticamente.\nCaminho: " + caminho,
                    "Falha ao abrir", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarCaminhoSemSuporte(Path caminho) {
        JOptionPane.showMessageDialog(janela,
                "A abertura automática não está disponível neste sistema.\nCaminho: " + caminho,
                "Local do resultado", JOptionPane.INFORMATION_MESSAGE);
    }

    static String[] criarArgumentos(ModoInterface modo, String seedTexto, String saidaTexto) {
        if (modo == null) {
            throw new IllegalArgumentException("Selecione um modo de execução.");
        }

        String seedNormalizada = seedTexto == null ? "" : seedTexto.trim();
        try {
            Long.parseLong(seedNormalizada);
        } catch (NumberFormatException excecao) {
            throw new IllegalArgumentException("A seed deve ser um número inteiro válido.", excecao);
        }

        if (saidaTexto == null || saidaTexto.isBlank()) {
            throw new IllegalArgumentException("Informe o arquivo CSV de saída.");
        }

        final Path saida;
        try {
            saida = garantirExtensaoCsv(Paths.get(saidaTexto.trim()).toAbsolutePath().normalize());
        } catch (InvalidPathException excecao) {
            throw new IllegalArgumentException("O caminho do arquivo de saída é inválido.", excecao);
        }

        if (modo.argumentoCli == null) {
            return new String[]{seedNormalizada, saida.toString()};
        }
        return new String[]{seedNormalizada, saida.toString(), modo.argumentoCli};
    }

    static Path caminhoPadrao(ModoInterface modo) {
        return Paths.get("resultados", modo.arquivoPadrao).toAbsolutePath().normalize();
    }

    private static Path garantirExtensaoCsv(Path caminho) {
        String nome = caminho.getFileName() == null ? "" : caminho.getFileName().toString();
        if (nome.toLowerCase().endsWith(".csv")) {
            return caminho;
        }
        return caminho.resolveSibling(nome + ".csv");
    }

    private static String mensagemErro(Throwable causa) {
        String mensagem = causa.getMessage();
        return mensagem == null || mensagem.isBlank()
                ? causa.getClass().getSimpleName()
                : mensagem;
    }

    enum ModoInterface {
        RAPIDO(
                "Rápido (~10 segundos)",
                "Verifica a instalação com 1 aquecimento, 3 medições e uma bateria reduzida.",
                "--rapido",
                "resultados_rapido.csv"),
        COMPLETO(
                "Completo (1 a 5 minutos)",
                "Executa a bateria integral usada na análise: 2 aquecimentos e 7 medições.",
                null,
                "resultados.csv"),
        ESCALABILIDADE(
                "Escalabilidade (limites empíricos)",
                "Usa tamanhos graduais, timeout de 5 segundos e interrupção adaptativa.",
                "--escalabilidade",
                "resultados_escalabilidade.csv");

        private final String rotulo;
        private final String descricao;
        private final String argumentoCli;
        private final String arquivoPadrao;

        ModoInterface(String rotulo, String descricao, String argumentoCli, String arquivoPadrao) {
            this.rotulo = rotulo;
            this.descricao = descricao;
            this.argumentoCli = argumentoCli;
            this.arquivoPadrao = arquivoPadrao;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    /** Converte a saida textual em blocos completos para atualizacao segura do Swing. */
    private static final class LineOutputStream extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Consumer<String> consumidor;

        private LineOutputStream(Consumer<String> consumidor) {
            this.consumidor = consumidor;
        }

        @Override
        public synchronized void write(int byteLido) {
            if (byteLido == '\n') {
                publicar(true);
            } else if (byteLido != '\r') {
                buffer.write(byteLido);
            }
        }

        @Override
        public synchronized void flush() {
            publicar(false);
        }

        @Override
        public synchronized void close() {
            publicar(false);
        }

        private void publicar(boolean adicionarQuebra) {
            if (buffer.size() == 0 && !adicionarQuebra) {
                return;
            }
            String texto = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            consumidor.accept(adicionarQuebra ? texto + System.lineSeparator() : texto);
        }
    }
}
