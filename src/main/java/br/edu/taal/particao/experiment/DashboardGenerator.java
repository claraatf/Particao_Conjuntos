package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Gera um dashboard HTML autocontido a partir dos registros experimentais.
 * CSS, JavaScript e dados ficam embutidos no proprio arquivo, permitindo que
 * ele seja aberto diretamente no navegador, sem servidor ou acesso a internet.
 */
public class DashboardGenerator {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z", Locale.forLanguageTag("pt-BR"));

    /**
     * Deriva o caminho do dashboard do caminho do CSV, mantendo ambos na
     * mesma pasta. Ex.: {@code resultados.csv -> resultados_dashboard.html}.
     */
    public static Path caminhoDashboard(Path arquivoCsv) {
        Objects.requireNonNull(arquivoCsv, "O caminho do CSV nao pode ser nulo.");
        Path nomePath = arquivoCsv.getFileName();
        if (nomePath == null) {
            throw new IllegalArgumentException("O caminho do CSV deve identificar um arquivo.");
        }
        String nome = nomePath.toString();
        int ultimoPonto = nome.lastIndexOf('.');
        String base = ultimoPonto > 0 ? nome.substring(0, ultimoPonto) : nome;
        return arquivoCsv.resolveSibling(base + "_dashboard.html");
    }

    /** Grava o dashboard no caminho informado usando UTF-8. */
    public void escrever(Path arquivo, List<ExecutionRecord> registros,
                         String descricaoModo, long seed) throws IOException {
        Objects.requireNonNull(arquivo, "O caminho do dashboard nao pode ser nulo.");
        Objects.requireNonNull(registros, "A lista de registros nao pode ser nula.");
        Objects.requireNonNull(descricaoModo, "A descricao do modo nao pode ser nula.");

        Path pai = arquivo.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }

        String html = TEMPLATE
                .replace("__MODO__", escaparHtml(descricaoModo))
                .replace("__SEED__", Long.toString(seed))
                .replace("__GERADO_EM__", escaparHtml(FORMATO_DATA.format(ZonedDateTime.now())))
                .replace("__DADOS__", serializarRegistros(registros));
        Files.writeString(arquivo, html, StandardCharsets.UTF_8);
    }

    private String serializarRegistros(List<ExecutionRecord> registros) {
        StringBuilder json = new StringBuilder(Math.max(256, registros.size() * 360));
        json.append('[');
        for (int i = 0; i < registros.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            adicionarRegistro(json, registros.get(i));
        }
        json.append(']');
        return json.toString();
    }

    private void adicionarRegistro(StringBuilder json, ExecutionRecord registro) {
        json.append('{');
        campo(json, "perfil", registro.getPerfil());
        campo(json, "tamanho", registro.getTamanho());
        campo(json, "instancia", registro.getNomeInstancia());
        campo(json, "algoritmo", registro.getNomeAlgoritmo());
        campo(json, "exato", registro.isExato());
        campo(json, "status", registro.getStatus().name());

        if (registro.isSucesso()) {
            PartitionResult resultado = registro.getResultado();
            Metrics metricas = resultado.getMetricas();
            campo(json, "diferenca", resultado.getDiferenca());
            campoNullable(json, "referencia", registro.getDiferencaOtimaReferencia());
            campo(json, "referenciaComprovada", registro.isReferenciaComprovada());
            campoNullable(json, "gapAbsoluto", registro.getGapAbsoluto());
            campoNullable(json, "gapPercentual", registro.getGapPercentual());
            campo(json, "desequilibrio", resultado.getDesequilibrioRelativo());
            campoNullable(json, "atingiuOtimo", registro.atingiuOtimo());
            campo(json, "tempoMs", metricas.getTempoExecucaoMillis());
            campo(json, "tempoMinMs", metricas.getTempoMinimoMillis());
            campo(json, "tempoMaxMs", metricas.getTempoMaximoMillis());
            campo(json, "desvioTempoMs", metricas.getDesvioPadraoTempoMillis());
            campo(json, "repeticoes", metricas.getRepeticoesMedidas());
            if (metricas.isMemoriaAlocadaDisponivel()) {
                campo(json, "memoriaMb", metricas.getMemoriaAlocadaMB());
            } else {
                campoNullable(json, "memoriaMb", null);
            }
            campo(json, "estados", metricas.getEstadosExplorados());
            campo(json, "chamadas", metricas.getChamadasRecursivas());
            campo(json, "podas", metricas.getPodasRealizadas());
            campo(json, "profundidade", metricas.getProfundidadeMaxima());
        } else {
            campoNullable(json, "diferenca", null);
            campoNullable(json, "referencia", null);
            campo(json, "referenciaComprovada", false);
            campoNullable(json, "gapAbsoluto", null);
            campoNullable(json, "gapPercentual", null);
            campoNullable(json, "desequilibrio", null);
            campoNullable(json, "atingiuOtimo", null);
            campoNullable(json, "tempoMs", null);
            campoNullable(json, "tempoMinMs", null);
            campoNullable(json, "tempoMaxMs", null);
            campoNullable(json, "desvioTempoMs", null);
            campoNullable(json, "repeticoes", null);
            campoNullable(json, "memoriaMb", null);
            campoNullable(json, "estados", null);
            campoNullable(json, "chamadas", null);
            campoNullable(json, "podas", null);
            campoNullable(json, "profundidade", null);
        }
        campoUltimo(json, "observacao", registro.getObservacao());
        json.append('}');
    }

    private void campo(StringBuilder json, String nome, String valor) {
        json.append(jsonString(nome)).append(':').append(jsonString(valor)).append(',');
    }

    private void campo(StringBuilder json, String nome, long valor) {
        json.append(jsonString(nome)).append(':').append(valor).append(',');
    }

    private void campo(StringBuilder json, String nome, double valor) {
        json.append(jsonString(nome)).append(':').append(numeroJson(valor)).append(',');
    }

    private void campo(StringBuilder json, String nome, boolean valor) {
        json.append(jsonString(nome)).append(':').append(valor).append(',');
    }

    private void campoNullable(StringBuilder json, String nome, Object valor) {
        json.append(jsonString(nome)).append(':');
        if (valor == null) {
            json.append("null");
        } else if (valor instanceof Double numero) {
            json.append(numeroJson(numero));
        } else if (valor instanceof Number || valor instanceof Boolean) {
            json.append(valor);
        } else {
            json.append(jsonString(valor.toString()));
        }
        json.append(',');
    }

    private void campoUltimo(StringBuilder json, String nome, String valor) {
        json.append(jsonString(nome)).append(':').append(jsonString(valor));
    }

    private String numeroJson(double valor) {
        return Double.isFinite(valor) ? String.format(Locale.US, "%.10f", valor) : "null";
    }

    /** Escapa tambem caracteres que poderiam encerrar o elemento script. */
    private String jsonString(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder resultado = new StringBuilder(valor.length() + 16);
        resultado.append('"');
        for (int i = 0; i < valor.length(); i++) {
            char caractere = valor.charAt(i);
            switch (caractere) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\b' -> resultado.append("\\b");
                case '\f' -> resultado.append("\\f");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                case '<' -> resultado.append("\\u003c");
                case '>' -> resultado.append("\\u003e");
                case '&' -> resultado.append("\\u0026");
                case '\u2028' -> resultado.append("\\u2028");
                case '\u2029' -> resultado.append("\\u2029");
                default -> {
                    if (caractere < 0x20) {
                        resultado.append(String.format(Locale.ROOT, "\\u%04x", (int) caractere));
                    } else {
                        resultado.append(caractere);
                    }
                }
            }
        }
        resultado.append('"');
        return resultado.toString();
    }

    private static String escaparHtml(String valor) {
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final String TEMPLATE = """
            <!doctype html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta name="color-scheme" content="light">
              <title>Dashboard — Partição de Conjuntos</title>
              <style>
                :root {
                  --bg: #f3f6fa;
                  --surface: #ffffff;
                  --surface-muted: #f8fafc;
                  --ink: #162033;
                  --muted: #607089;
                  --line: #dbe3ee;
                  --navy: #10233f;
                  --blue: #2563eb;
                  --teal: #0f766e;
                  --amber: #d97706;
                  --red: #dc2626;
                  --violet: #7c3aed;
                  --shadow: 0 14px 38px rgba(16, 35, 63, 0.08);
                  --radius: 18px;
                }
                * { box-sizing: border-box; }
                html { scroll-behavior: smooth; }
                body {
                  margin: 0;
                  color: var(--ink);
                  background: var(--bg);
                  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont,
                               "Segoe UI", sans-serif;
                  line-height: 1.45;
                }
                header {
                  color: #fff;
                  background:
                    radial-gradient(circle at 88% 15%, rgba(56, 189, 248, .22), transparent 30%),
                    linear-gradient(135deg, #0b1b32 0%, #15365e 62%, #0f5963 100%);
                  padding: 38px 0 74px;
                }
                .wrap { width: min(1480px, calc(100% - 40px)); margin: 0 auto; }
                .eyebrow {
                  margin: 0 0 8px;
                  color: #7dd3fc;
                  font-size: .76rem;
                  font-weight: 800;
                  letter-spacing: .14em;
                  text-transform: uppercase;
                }
                h1 { margin: 0; font-size: clamp(2rem, 5vw, 3.65rem); line-height: 1.04; }
                .subtitle { max-width: 760px; margin: 14px 0 0; color: #d7e7f7; font-size: 1.02rem; }
                .run-meta { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 24px; }
                .run-meta span {
                  padding: 7px 11px;
                  border: 1px solid rgba(255,255,255,.18);
                  border-radius: 999px;
                  background: rgba(255,255,255,.08);
                  color: #eaf5ff;
                  font-size: .82rem;
                }
                main { margin-top: -45px; padding-bottom: 64px; }
                .panel {
                  margin-bottom: 20px;
                  padding: 22px;
                  border: 1px solid rgba(219, 227, 238, .9);
                  border-radius: var(--radius);
                  background: var(--surface);
                  box-shadow: var(--shadow);
                }
                .filters { display: grid; grid-template-columns: 1.3fr repeat(3, 1fr) auto; gap: 14px; align-items: end; }
                .filter-intro h2 { margin: 0 0 4px; font-size: 1.05rem; }
                .filter-intro p { margin: 0; color: var(--muted); font-size: .82rem; }
                label { display: grid; gap: 6px; color: #475569; font-size: .73rem; font-weight: 800; text-transform: uppercase; letter-spacing: .06em; }
                select, button {
                  min-height: 42px;
                  border: 1px solid #cbd5e1;
                  border-radius: 10px;
                  background: #fff;
                  color: var(--ink);
                  font: inherit;
                }
                select { width: 100%; padding: 0 36px 0 11px; }
                button { padding: 0 15px; cursor: pointer; font-weight: 750; }
                button:hover { border-color: #94a3b8; background: #f8fafc; }
                button:focus-visible, select:focus-visible { outline: 3px solid rgba(37,99,235,.22); outline-offset: 2px; }
                button:disabled { cursor: not-allowed; opacity: .45; }
                .cards { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; margin-bottom: 20px; }
                .card {
                  position: relative;
                  overflow: hidden;
                  min-height: 126px;
                  padding: 20px;
                  border: 1px solid var(--line);
                  border-radius: 16px;
                  background: var(--surface);
                  box-shadow: 0 8px 28px rgba(16,35,63,.05);
                }
                .card::after { content: ""; position: absolute; inset: 0 auto 0 0; width: 4px; background: var(--accent, var(--blue)); }
                .card-label { color: var(--muted); font-size: .74rem; font-weight: 800; text-transform: uppercase; letter-spacing: .06em; }
                .card-value { margin-top: 7px; font-size: clamp(1.55rem, 3vw, 2.25rem); font-weight: 850; letter-spacing: -.04em; }
                .card-note { margin-top: 2px; color: var(--muted); font-size: .77rem; }
                .section-title { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
                .section-title h2 { margin: 0; font-size: 1.18rem; }
                .section-title p { margin: 4px 0 0; color: var(--muted); font-size: .82rem; }
                .charts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
                .chart-panel { min-width: 0; }
                canvas { display: block; width: 100%; height: 330px; }
                .legend-note { color: var(--muted); font-size: .76rem; }
                .table-scroll { overflow: auto; border: 1px solid var(--line); border-radius: 13px; }
                table { width: 100%; border-collapse: collapse; font-size: .8rem; white-space: nowrap; }
                th, td { padding: 11px 12px; border-bottom: 1px solid #e8edf4; text-align: right; }
                th { position: sticky; top: 0; z-index: 1; background: #eef3f8; color: #526179; font-size: .7rem; text-transform: uppercase; letter-spacing: .05em; }
                th:first-child, td:first-child, th:nth-child(2), td:nth-child(2), th:nth-child(3), td:nth-child(3) { text-align: left; }
                tr:last-child td { border-bottom: 0; }
                tbody tr:hover { background: #f8fafc; }
                .algorithm-table td:nth-child(1) { font-weight: 780; }
                .status {
                  display: inline-block;
                  padding: 4px 8px;
                  border-radius: 999px;
                  font-size: .68rem;
                  font-weight: 850;
                  letter-spacing: .03em;
                }
                .status-SUCESSO { color: #0b655e; background: #ccfbf1; }
                .status-TEMPO_LIMITE { color: #92400e; background: #fef3c7; }
                .status-MEMORIA_INVIAVEL { color: #9f1239; background: #ffe4e6; }
                .status-NAO_EXECUTADO { color: #5b21b6; background: #ede9fe; }
                .status-ERRO { color: #991b1b; background: #fee2e2; }
                .pagination { display: flex; align-items: center; justify-content: flex-end; gap: 10px; margin-top: 14px; }
                .pagination span { min-width: 150px; text-align: center; color: var(--muted); font-size: .8rem; }
                .empty { color: var(--muted); text-align: center !important; padding: 30px !important; }
                footer { color: var(--muted); text-align: center; font-size: .76rem; padding: 12px 0 0; }
                @media (max-width: 1050px) {
                  .filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                  .filter-intro { grid-column: 1 / -1; }
                  .cards { grid-template-columns: repeat(3, minmax(0, 1fr)); }
                }
                @media (max-width: 760px) {
                  .wrap { width: min(100% - 24px, 1480px); }
                  header { padding-top: 28px; }
                  .filters, .charts { grid-template-columns: 1fr; }
                  .cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
                  .panel { padding: 17px; }
                  canvas { height: 300px; }
                }
                @media (max-width: 440px) { .cards { grid-template-columns: 1fr; } }
                @media print {
                  body { background: #fff; }
                  header { padding: 20px 0; background: var(--navy); }
                  main { margin-top: 18px; }
                  .filters, .pagination, footer { display: none; }
                  .panel, .card { box-shadow: none; break-inside: avoid; }
                  .charts { grid-template-columns: 1fr; }
                }
              </style>
            </head>
            <body>
              <header>
                <div class="wrap">
                  <p class="eyebrow">TAAL · análise experimental</p>
                  <h1>Partição de Conjuntos</h1>
                  <p class="subtitle">Dashboard comparativo das estratégias exatas e heurísticas, com foco em desempenho, escalabilidade e qualidade das soluções.</p>
                  <div class="run-meta">
                    <span>Modo: __MODO__</span>
                    <span>Seed: __SEED__</span>
                    <span>Gerado em: __GERADO_EM__</span>
                    <span>Arquivo autônomo · sem internet</span>
                  </div>
                </div>
              </header>

              <main class="wrap">
                <section class="panel filters" aria-labelledby="titulo-filtros">
                  <div class="filter-intro">
                    <h2 id="titulo-filtros">Recorte dos dados</h2>
                    <p>Todos os indicadores e gráficos respondem aos filtros.</p>
                  </div>
                  <label>Perfil<select id="filter-profile" aria-label="Filtrar por perfil"></select></label>
                  <label>Algoritmo<select id="filter-algorithm" aria-label="Filtrar por algoritmo"></select></label>
                  <label>Status<select id="filter-status" aria-label="Filtrar por status"></select></label>
                  <button id="clear-filters" type="button">Limpar filtros</button>
                </section>

                <section class="cards" aria-label="Indicadores gerais">
                  <article class="card" style="--accent:#2563eb"><div class="card-label">Registros</div><div class="card-value" id="kpi-total">0</div><div class="card-note">combinações no recorte</div></article>
                  <article class="card" style="--accent:#0f766e"><div class="card-label">Taxa de sucesso</div><div class="card-value" id="kpi-success">0%</div><div class="card-note" id="kpi-success-note">0 conclusões</div></article>
                  <article class="card" style="--accent:#d97706"><div class="card-label">Tempo mediano</div><div class="card-value" id="kpi-time">—</div><div class="card-note">entre execuções concluídas</div></article>
                  <article class="card" style="--accent:#7c3aed"><div class="card-label">Memória mediana</div><div class="card-value" id="kpi-memory">—</div><div class="card-note">heap alocado pela thread</div></article>
                  <article class="card" style="--accent:#dc2626"><div class="card-label">Limites observados</div><div class="card-value" id="kpi-limits">0</div><div class="card-note">timeout ou memória inviável</div></article>
                </section>

                <section class="panel" aria-labelledby="titulo-comparacao">
                  <div class="section-title"><div><h2 id="titulo-comparacao">Comparação consolidada</h2><p>Tempos e memória usam a mediana das execuções concluídas.</p></div></div>
                  <div class="table-scroll">
                    <table class="algorithm-table">
                      <thead><tr><th>Algoritmo</th><th>Registros</th><th>Sucessos</th><th>Sucesso %</th><th>Tempo mediano</th><th>Memória mediana</th><th>Ótimos na base comum %</th></tr></thead>
                      <tbody id="algorithm-summary"></tbody>
                    </table>
                  </div>
                </section>

                <section class="charts" aria-label="Gráficos comparativos">
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Tempo por tamanho</h2><p>Mediana por algoritmo; eixo vertical logarítmico.</p></div></div><canvas id="chart-time" role="img" aria-label="Gráfico de tempo por tamanho"></canvas></article>
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Memória por tamanho</h2><p>Heap alocado pela thread; eixo vertical logarítmico.</p></div></div><canvas id="chart-memory" role="img" aria-label="Gráfico de memória por tamanho"></canvas></article>
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Estados explorados</h2><p>Crescimento do espaço de busca em escala logarítmica.</p></div></div><canvas id="chart-states" role="img" aria-label="Gráfico de estados explorados por tamanho"></canvas></article>
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Distribuição dos status</h2><p>Cobertura, limites empíricos e cortes planejados.</p></div></div><canvas id="chart-status" role="img" aria-label="Gráfico de status por algoritmo"></canvas></article>
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Qualidade das soluções</h2><p>Desequilíbrio relativo médio sobre a base comum aos cinco algoritmos.</p></div></div><canvas id="chart-quality" role="img" aria-label="Gráfico de qualidade por algoritmo"></canvas></article>
                  <article class="panel chart-panel"><div class="section-title"><div><h2>Taxa de ótimo comprovado</h2><p>Percentual de ótimos sobre a mesma base comparável entre algoritmos.</p></div></div><canvas id="chart-optimum" role="img" aria-label="Gráfico de taxa de ótimo por algoritmo"></canvas></article>
                </section>

                <section class="panel" aria-labelledby="titulo-detalhes">
                  <div class="section-title"><div><h2 id="titulo-detalhes">Execuções detalhadas</h2><p id="table-caption">0 registros</p></div><span class="legend-note">Passe o cursor sobre o status para ler a observação.</span></div>
                  <div class="table-scroll">
                    <table>
                      <thead><tr><th>Perfil</th><th>Instância</th><th>Algoritmo</th><th>n</th><th>Status</th><th>Diferença</th><th>Tempo</th><th>Memória</th><th>Estados</th><th>Desequilíbrio</th></tr></thead>
                      <tbody id="details-body"></tbody>
                    </table>
                  </div>
                  <div class="pagination"><button id="prev-page" type="button">Anterior</button><span id="page-info">Página 1</span><button id="next-page" type="button">Próxima</button></div>
                </section>
                <footer>Dashboard gerado localmente pelo projeto Partição de Conjuntos · Técnicas de Análise de Algoritmos</footer>
              </main>

              <script>
                "use strict";
                const RAW_DATA = Object.freeze(__DADOS__);
                const PAGE_SIZE = 100;
                const ALGORITHM_COLORS = {
                  Backtracking: "#2563eb", BranchAndBound: "#7c3aed",
                  ProgramacaoDinamica: "#0f766e", Guloso: "#d97706", KarmarkarKarp: "#dc2626"
                };
                const STATUS_COLORS = {
                  SUCESSO: "#0f766e", TEMPO_LIMITE: "#d97706", MEMORIA_INVIAVEL: "#e11d48",
                  NAO_EXECUTADO: "#7c3aed", ERRO: "#b91c1c"
                };
                const STATUS_ORDER = ["SUCESSO", "TEMPO_LIMITE", "MEMORIA_INVIAVEL", "NAO_EXECUTADO", "ERRO"];
                const state = { page: 0 };

                const byId = id => document.getElementById(id);
                const unique = values => [...new Set(values)].sort((a, b) => String(a).localeCompare(String(b), "pt-BR"));
                const median = values => {
                  const clean = values.filter(Number.isFinite).sort((a, b) => a - b);
                  if (!clean.length) return null;
                  const middle = Math.floor(clean.length / 2);
                  return clean.length % 2 ? clean[middle] : (clean[middle - 1] + clean[middle]) / 2;
                };
                const mean = values => {
                  const clean = values.filter(Number.isFinite);
                  return clean.length ? clean.reduce((sum, value) => sum + value, 0) / clean.length : null;
                };
                const formatInteger = value => Number.isFinite(value) ? Math.round(value).toLocaleString("pt-BR") : "—";
                const formatPercent = value => Number.isFinite(value) ? value.toLocaleString("pt-BR", {maximumFractionDigits: 1}) + "%" : "—";
                const formatMetric = (value, unit = "") => {
                  if (!Number.isFinite(value)) return "—";
                  if (value === 0) return "0" + unit;
                  if (Math.abs(value) >= 1e9) return (value / 1e9).toLocaleString("pt-BR", {maximumFractionDigits: 1}) + " bi" + unit;
                  if (Math.abs(value) >= 1e6) return (value / 1e6).toLocaleString("pt-BR", {maximumFractionDigits: 1}) + " mi" + unit;
                  if (Math.abs(value) >= 1e3) return (value / 1e3).toLocaleString("pt-BR", {maximumFractionDigits: 1}) + " mil" + unit;
                  if (Math.abs(value) < .01) return value.toExponential(1) + unit;
                  return value.toLocaleString("pt-BR", {maximumFractionDigits: 2}) + unit;
                };
                const escapeHtml = value => String(value ?? "")
                  .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
                  .replaceAll('"', "&quot;").replaceAll("'", "&#39;");

                function fillSelect(id, label, values) {
                  const select = byId(id);
                  select.innerHTML = `<option value="">${label}</option>` + values
                    .map(value => `<option value="${escapeHtml(value)}">${escapeHtml(value)}</option>`).join("");
                }

                function currentData() {
                  const profile = byId("filter-profile").value;
                  const algorithm = byId("filter-algorithm").value;
                  const status = byId("filter-status").value;
                  return RAW_DATA.filter(row => (!profile || row.perfil === profile)
                    && (!algorithm || row.algoritmo === algorithm)
                    && (!status || row.status === status));
                }

                function selectedAlgorithms(data) {
                  const selected = byId("filter-algorithm").value;
                  return selected ? [selected] : unique(data.map(row => row.algoritmo));
                }

                function commonBaseInstances() {
                  const profile = byId("filter-profile").value;
                  const scope = RAW_DATA.filter(row => !profile || row.perfil === profile);
                  const allAlgorithms = unique(RAW_DATA.map(row => row.algoritmo));
                  const groups = new Map();
                  scope.forEach(row => {
                    if (!groups.has(row.instancia)) groups.set(row.instancia, []);
                    groups.get(row.instancia).push(row);
                  });
                  return new Set([...groups.entries()].filter(([, rows]) =>
                    rows.some(row => row.referenciaComprovada)
                    && allAlgorithms.every(algorithm => rows.some(row => row.algoritmo === algorithm && row.status === "SUCESSO")))
                    .map(([instance]) => instance));
                }

                function renderKpis(data) {
                  const successes = data.filter(row => row.status === "SUCESSO");
                  const limits = data.filter(row => row.status === "TEMPO_LIMITE" || row.status === "MEMORIA_INVIAVEL").length;
                  byId("kpi-total").textContent = formatInteger(data.length);
                  byId("kpi-success").textContent = formatPercent(data.length ? 100 * successes.length / data.length : null);
                  byId("kpi-success-note").textContent = `${formatInteger(successes.length)} conclusões`;
                  byId("kpi-time").textContent = formatMetric(median(successes.map(row => row.tempoMs)), " ms");
                  byId("kpi-memory").textContent = formatMetric(median(successes.map(row => row.memoriaMb)), " MB");
                  byId("kpi-limits").textContent = formatInteger(limits);
                }

                function renderAlgorithmSummary(data) {
                  const body = byId("algorithm-summary");
                  const algorithms = selectedAlgorithms(data);
                  const commonBase = commonBaseInstances();
                  if (!algorithms.length) {
                    body.innerHTML = '<tr><td colspan="7" class="empty">Nenhum registro no recorte selecionado.</td></tr>';
                    return;
                  }
                  body.innerHTML = algorithms.map(algorithm => {
                    const rows = data.filter(row => row.algoritmo === algorithm);
                    const success = rows.filter(row => row.status === "SUCESSO");
                    const comparable = success.filter(row => commonBase.has(row.instancia));
                    const optimal = comparable.filter(row => row.atingiuOtimo === true).length;
                    return `<tr><td>${escapeHtml(algorithm)}</td><td>${formatInteger(rows.length)}</td>`
                      + `<td>${formatInteger(success.length)}</td><td>${formatPercent(rows.length ? 100 * success.length / rows.length : null)}</td>`
                      + `<td>${formatMetric(median(success.map(row => row.tempoMs)), " ms")}</td>`
                      + `<td>${formatMetric(median(success.map(row => row.memoriaMb)), " MB")}</td>`
                      + `<td>${formatPercent(comparable.length ? 100 * optimal / comparable.length : null)}</td></tr>`;
                  }).join("");
                }

                function canvasContext(id) {
                  const canvas = byId(id);
                  const ratio = window.devicePixelRatio || 1;
                  const width = Math.max(canvas.clientWidth, 300);
                  const height = Math.max(canvas.clientHeight, 260);
                  canvas.width = Math.round(width * ratio);
                  canvas.height = Math.round(height * ratio);
                  const ctx = canvas.getContext("2d");
                  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
                  ctx.clearRect(0, 0, width, height);
                  return {ctx, width, height};
                }

                function drawEmpty(ctx, width, height, message = "Sem dados para este recorte") {
                  ctx.fillStyle = "#64748b";
                  ctx.font = "14px system-ui";
                  ctx.textAlign = "center";
                  ctx.fillText(message, width / 2, height / 2);
                }

                function groupedSeries(data, key) {
                  const success = data.filter(row => row.status === "SUCESSO" && Number.isFinite(row[key]));
                  const algorithms = selectedAlgorithms(data);
                  return algorithms.map(algorithm => {
                    const rows = success.filter(row => row.algoritmo === algorithm);
                    const sizes = unique(rows.map(row => row.tamanho)).sort((a, b) => a - b);
                    return {
                      name: algorithm,
                      color: ALGORITHM_COLORS[algorithm] || "#475569",
                      points: sizes.map(size => ({x: size, y: median(rows.filter(row => row.tamanho === size).map(row => row[key]))}))
                    };
                  }).filter(series => series.points.length);
                }

                function drawLineChart(id, data, key, unit) {
                  const {ctx, width, height} = canvasContext(id);
                  const series = groupedSeries(data, key);
                  if (!series.length) return drawEmpty(ctx, width, height);
                  const points = series.flatMap(item => item.points);
                  const sizes = unique(points.map(point => point.x)).sort((a, b) => a - b);
                  const transformed = points.map(point => Math.log10(Math.max(point.y, 1e-9)));
                  let minY = Math.min(...transformed);
                  let maxY = Math.max(...transformed);
                  if (minY === maxY) { minY -= .5; maxY += .5; }
                  const pad = {left: 64, right: 18, top: 44, bottom: 45};
                  const plotW = width - pad.left - pad.right;
                  const plotH = height - pad.top - pad.bottom;
                  const xAt = x => sizes.length === 1 ? pad.left + plotW / 2 : pad.left + sizes.indexOf(x) * plotW / (sizes.length - 1);
                  const yAt = y => pad.top + (maxY - Math.log10(Math.max(y, 1e-9))) * plotH / (maxY - minY);
                  ctx.strokeStyle = "#dbe3ee";
                  ctx.lineWidth = 1;
                  ctx.font = "11px system-ui";
                  ctx.fillStyle = "#64748b";
                  for (let i = 0; i <= 4; i++) {
                    const y = pad.top + i * plotH / 4;
                    const logValue = maxY - i * (maxY - minY) / 4;
                    ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.stroke();
                    ctx.textAlign = "right"; ctx.fillText(formatMetric(10 ** logValue, unit), pad.left - 8, y + 4);
                  }
                  const labelStep = Math.max(1, Math.ceil(sizes.length / 8));
                  sizes.forEach((size, index) => {
                    if (index % labelStep === 0 || index === sizes.length - 1) {
                      ctx.textAlign = "center"; ctx.fillText(String(size), xAt(size), height - 19);
                    }
                  });
                  ctx.textAlign = "center"; ctx.fillText("Tamanho da instância (n)", pad.left + plotW / 2, height - 2);
                  series.forEach(item => {
                    ctx.strokeStyle = item.color; ctx.fillStyle = item.color; ctx.lineWidth = 2.2;
                    ctx.beginPath();
                    item.points.forEach((point, index) => index ? ctx.lineTo(xAt(point.x), yAt(point.y)) : ctx.moveTo(xAt(point.x), yAt(point.y)));
                    ctx.stroke();
                    item.points.forEach(point => { ctx.beginPath(); ctx.arc(xAt(point.x), yAt(point.y), 3.2, 0, Math.PI * 2); ctx.fill(); });
                  });
                  drawLegend(ctx, series.map(item => ({label: item.name, color: item.color})), width);
                }

                function drawLegend(ctx, items, width) {
                  ctx.font = "11px system-ui";
                  let x = 10;
                  let y = 15;
                  items.forEach(item => {
                    const itemWidth = ctx.measureText(item.label).width + 25;
                    if (x + itemWidth > width - 8) { x = 10; y += 17; }
                    ctx.fillStyle = item.color; ctx.fillRect(x, y - 8, 10, 10);
                    ctx.fillStyle = "#475569"; ctx.textAlign = "left"; ctx.fillText(item.label, x + 14, y);
                    x += itemWidth;
                  });
                }

                function drawStatusChart(data) {
                  const {ctx, width, height} = canvasContext("chart-status");
                  const algorithms = selectedAlgorithms(data);
                  if (!algorithms.length) return drawEmpty(ctx, width, height);
                  const pad = {left: 142, right: 22, top: 55, bottom: 20};
                  const plotW = width - pad.left - pad.right;
                  const rowH = (height - pad.top - pad.bottom) / algorithms.length;
                  const totals = algorithms.map(algorithm => data.filter(row => row.algoritmo === algorithm).length);
                  const maxTotal = Math.max(...totals, 1);
                  algorithms.forEach((algorithm, rowIndex) => {
                    const y = pad.top + rowIndex * rowH + rowH * .24;
                    const barH = Math.min(28, rowH * .52);
                    ctx.fillStyle = "#475569"; ctx.font = "11px system-ui"; ctx.textAlign = "right";
                    ctx.fillText(algorithm, pad.left - 10, y + barH * .72);
                    let x = pad.left;
                    STATUS_ORDER.forEach(status => {
                      const count = data.filter(row => row.algoritmo === algorithm && row.status === status).length;
                      const segment = count * plotW / maxTotal;
                      if (segment > 0) {
                        ctx.fillStyle = STATUS_COLORS[status]; ctx.fillRect(x, y, segment, barH);
                        if (segment > 24) { ctx.fillStyle = "#fff"; ctx.textAlign = "center"; ctx.font = "bold 10px system-ui"; ctx.fillText(String(count), x + segment / 2, y + barH * .7); }
                        x += segment;
                      }
                    });
                  });
                  drawLegend(ctx, STATUS_ORDER.map(status => ({label: status, color: STATUS_COLORS[status]})), width);
                }

                function drawBarChart(id, data, metric, unit, color) {
                  const {ctx, width, height} = canvasContext(id);
                  const algorithms = selectedAlgorithms(data);
                  const commonBase = commonBaseInstances();
                  const rows = algorithms.map(algorithm => {
                    const success = data.filter(row => row.algoritmo === algorithm && row.status === "SUCESSO" && commonBase.has(row.instancia));
                    if (metric === "optimal") {
                      const eligible = success.filter(row => row.atingiuOtimo !== null);
                      const value = eligible.length ? 100 * eligible.filter(row => row.atingiuOtimo === true).length / eligible.length : null;
                      return {label: algorithm, value};
                    }
                    return {label: algorithm, value: mean(success.map(row => row.desequilibrio))};
                  }).filter(row => Number.isFinite(row.value));
                  if (!rows.length) return drawEmpty(ctx, width, height);
                  const pad = {left: 46, right: 18, top: 25, bottom: 82};
                  const plotW = width - pad.left - pad.right;
                  const plotH = height - pad.top - pad.bottom;
                  const maxValue = Math.max(...rows.map(row => row.value), metric === "optimal" ? 100 : 0.0001);
                  const slot = plotW / rows.length;
                  rows.forEach((row, index) => {
                    const barW = Math.min(64, slot * .6);
                    const barH = row.value * plotH / maxValue;
                    const x = pad.left + index * slot + (slot - barW) / 2;
                    const y = pad.top + plotH - barH;
                    ctx.fillStyle = ALGORITHM_COLORS[row.label] || color;
                    ctx.fillRect(x, y, barW, barH);
                    ctx.fillStyle = "#334155"; ctx.textAlign = "center"; ctx.font = "bold 11px system-ui";
                    ctx.fillText(formatMetric(row.value, unit), x + barW / 2, Math.max(15, y - 7));
                    ctx.save(); ctx.translate(x + barW / 2, height - 10); ctx.rotate(-Math.PI / 5);
                    ctx.font = "11px system-ui"; ctx.textAlign = "right"; ctx.fillText(row.label, 0, 0); ctx.restore();
                  });
                  ctx.strokeStyle = "#cbd5e1"; ctx.beginPath(); ctx.moveTo(pad.left, pad.top + plotH); ctx.lineTo(width - pad.right, pad.top + plotH); ctx.stroke();
                }

                function renderDetails(data) {
                  const totalPages = Math.max(1, Math.ceil(data.length / PAGE_SIZE));
                  state.page = Math.min(state.page, totalPages - 1);
                  const start = state.page * PAGE_SIZE;
                  const pageRows = data.slice(start, start + PAGE_SIZE);
                  const body = byId("details-body");
                  body.innerHTML = pageRows.length ? pageRows.map(row => `<tr>`
                    + `<td>${escapeHtml(row.perfil)}</td><td>${escapeHtml(row.instancia)}</td><td>${escapeHtml(row.algoritmo)}</td>`
                    + `<td>${formatInteger(row.tamanho)}</td><td><span class="status status-${escapeHtml(row.status)}" title="${escapeHtml(row.observacao || row.status)}">${escapeHtml(row.status)}</span></td>`
                    + `<td>${formatInteger(row.diferenca)}</td><td>${formatMetric(row.tempoMs, " ms")}</td><td>${formatMetric(row.memoriaMb, " MB")}</td>`
                    + `<td>${formatInteger(row.estados)}</td><td>${formatMetric(row.desequilibrio, "%")}</td></tr>`).join("")
                    : '<tr><td colspan="10" class="empty">Nenhuma execução corresponde aos filtros.</td></tr>';
                  byId("table-caption").textContent = `${formatInteger(data.length)} registros no recorte`;
                  byId("page-info").textContent = `Página ${state.page + 1} de ${totalPages}`;
                  byId("prev-page").disabled = state.page === 0;
                  byId("next-page").disabled = state.page >= totalPages - 1;
                }

                function renderAll(resetPage = false) {
                  if (resetPage) state.page = 0;
                  const data = currentData();
                  renderKpis(data);
                  renderAlgorithmSummary(data);
                  drawLineChart("chart-time", data, "tempoMs", " ms");
                  drawLineChart("chart-memory", data, "memoriaMb", " MB");
                  drawLineChart("chart-states", data, "estados", "");
                  drawStatusChart(data);
                  drawBarChart("chart-quality", data, "quality", "%", "#0f766e");
                  drawBarChart("chart-optimum", data, "optimal", "%", "#2563eb");
                  renderDetails(data);
                }

                fillSelect("filter-profile", "Todos os perfis", unique(RAW_DATA.map(row => row.perfil)));
                fillSelect("filter-algorithm", "Todos os algoritmos", unique(RAW_DATA.map(row => row.algoritmo)));
                fillSelect("filter-status", "Todos os status", STATUS_ORDER.filter(status => RAW_DATA.some(row => row.status === status)));
                ["filter-profile", "filter-algorithm", "filter-status"].forEach(id => byId(id).addEventListener("change", () => renderAll(true)));
                byId("clear-filters").addEventListener("click", () => {
                  ["filter-profile", "filter-algorithm", "filter-status"].forEach(id => byId(id).value = "");
                  renderAll(true);
                });
                byId("prev-page").addEventListener("click", () => { state.page--; renderAll(); });
                byId("next-page").addEventListener("click", () => { state.page++; renderAll(); });
                let resizeTimer;
                window.addEventListener("resize", () => { clearTimeout(resizeTimer); resizeTimer = setTimeout(() => renderAll(), 120); });
                renderAll();
              </script>
            </body>
            </html>
            """;
}
