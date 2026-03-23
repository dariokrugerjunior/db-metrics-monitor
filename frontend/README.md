# DB Metrics Monitor

Uma plataforma completa de observabilidade para bancos de dados PostgreSQL com foco em análise de locks, queries e performance.

## 🎨 Design System

### Estilo Visual
- **Dark Mode**: Interface escura profissional
- **Tipografia**: Inter (Google Fonts)
- **Inspiração**: Grafana, Datadog, Linear App, Vercel Dashboard

### Paleta de Cores

#### Status Colors
| Cor | Hex | Uso |
|-----|-----|-----|
| 🟢 Verde | `#10b981` | Status saudável / Healthy |
| 🟡 Amarelo | `#f59e0b` | Atenção / Warning |
| 🔴 Vermelho | `#ef4444` | Crítico / Critical |
| 🔵 Azul | `#3b82f6` | Informativo / Primary |
| 🟣 Roxo | `#8b5cf6` | Secundário / Charts |

#### Background Colors
| Elemento | Hex | Uso |
|----------|-----|-----|
| Background | `#0a0a0f` | Fundo principal |
| Card | `#111116` | Cards e containers |
| Secondary | `#1f1f28` | Backgrounds secundários |
| Border | `#27272a` | Bordas e divisores |
| Muted | `#71717a` | Texto secundário |
| Foreground | `#e4e4e7` | Texto principal |

## 📊 Funcionalidades

### 1. Dashboard Principal
- Visão geral em tempo real do sistema
- Métricas de conexões, locks, queries, CPU e memória
- Gráficos de tendência temporal
- Top queries e locks recentes
- Filtro de intervalo de tempo

### 2. Tela de Locks
- Visualização de todos os locks ativos
- Detalhes completos de cada lock
- Filtros e busca avançada
- Kill session com confirmação
- Alertas para locks críticos
- Indicador de severidade

### 3. Tela de Queries
- **Top Queries**: Queries mais executadas
- **Slow Queries**: Queries lentas que precisam otimização
- **Running Queries**: Queries em execução no momento
- Métricas de execução, tempo médio e total
- Sistema de busca e filtros
- Análise de performance

### 4. Tela de Conexões
- Métricas de conexões (Total, Ativas, Idle, Idle in Transaction)
- Gráficos de tendência temporal
- Distribuição por usuário e aplicação
- Análise detalhada de uso
- Gráfico de pizza para distribuição

### 5. Métricas do Sistema
- CPU Usage (gráfico de área com breakdown system/user)
- Memory Usage (Heap vs Non-Heap)
- Thread Activity (Active, Waiting, Blocked)
- Disk I/O (Read/Write)
- Barras de progresso de recursos
- Informações do sistema

### 6. Overview Consolidado
- Status geral do sistema (banner de alerta)
- Gauges de performance animados
- Alertas ativos priorizados
- Locks críticos em destaque
- Queries lentas que requerem atenção
- Status dos componentes da aplicação

## 🧩 Componentes Reutilizáveis

### Core Components

#### MetricCard
Card de métrica com suporte a ícones, badges de status e mudanças percentuais.
```tsx
<MetricCard
  title="CPU Usage"
  value="62%"
  change="+8% from last hour"
  changeType="warning"
  icon={Cpu}
  status="warning"
/>
```

#### DataTable
Tabela com:
- Sorting em colunas
- Renderização customizada
- Eventos de clique
- Empty states
- Responsividade

#### StatusBadge
Badge colorido para status:
- healthy, warning, critical
- blocked, blocking
- running, idle

#### PerformanceGauge
Gauge circular animado para métricas de performance (0-100%).

#### ProgressBar
Barra de progresso horizontal com cores dinâmicas baseadas no valor.

#### LiveActivityIndicator
Indicador de atividade ao vivo com animação pulsante.

#### TimeRangeFilter
Dropdown para filtrar dados por intervalo de tempo.

### Dialog Components
- Dialog para detalhes
- AlertDialog para confirmações críticas
- Suporte a animações (Motion)

## 🎭 Interações e Animações

- ✅ Hover states em todos os elementos
- ✅ Loading states (Skeleton)
- ✅ Empty states
- ✅ Confirmação antes de ações críticas
- ✅ Toast notifications (Sonner)
- ✅ Animações sutis com Motion/Framer
- ✅ Transições suaves entre páginas
- ✅ Indicador de atividade ao vivo
- ✅ Gauges animados
- ✅ Barras de progresso animadas
- ✅ Scrollbar customizada

## 🛠 Tecnologias

- **React 18.3** com TypeScript
- **React Router 7** para navegação
- **Tailwind CSS 4** para estilização
- **Recharts** para gráficos
- **Motion** (Framer Motion) para animações
- **Lucide React** para ícones
- **Radix UI** para componentes acessíveis
- **Sonner** para notificações

## 📱 Responsividade

- Desktop-first approach
- Grid responsivo (1 coluna mobile → 2-3 colunas tablet → 3-4 colunas desktop)
- Sidebar fixa em desktop
- Tabelas com scroll horizontal em mobile
- Componentes adaptam-se automaticamente

## 🎯 Público-Alvo

- SRE (Site Reliability Engineers)
- DevOps Engineers
- Backend Engineers
- Database Administrators

## 🚀 Como Usar

1. Navegue pelas diferentes telas usando a sidebar
2. Monitore métricas em tempo real
3. Analise queries lentas na aba Queries
4. Gerencie locks críticos na aba Locks
5. Acompanhe conexões e recursos do sistema
6. Use o filtro de tempo para análise histórica

## 🎨 Customização

### Alterando Cores
As cores principais estão definidas em `/src/styles/theme.css`. Para customizar:

1. Altere as variáveis CSS em `:root`
2. Cores de status em `--success`, `--warning`, `--destructive`
3. Cores de fundo em `--background`, `--card`, `--border`

### Adicionando Novos Componentes
1. Crie em `/src/app/components/`
2. Use o design system existente
3. Aplique animações com Motion quando apropriado

## 📝 Notas

- Dados são mockados para demonstração
- Em produção, conectar a uma API real de monitoramento
- Sistema projetado para alta performance e usabilidade
- Dark mode sempre ativo
- Scrollbar customizada para tema dark

## 🔄 Próximos Passos Sugeridos

- [ ] Integração com API real do PostgreSQL
- [ ] Autenticação e autorização
- [ ] Exportação de dados e relatórios
- [ ] Configuração de alertas personalizados
- [ ] Dashboard personalizável com drag-and-drop
- [ ] Histórico de métricas com maior granularidade
- [ ] Comparação de períodos
- [ ] Modo claro (light mode toggle)