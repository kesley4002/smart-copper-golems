# Smart Copper Golems Extended — primeiro protótipo

Base: GameMech007, branch `v-26.2`, commit `721ec7728577dcd36359a138afc94b2f3197ba9b`.
Branch pretendida no fork: `feature/smart-categories-26.2`.

## Estado real da entrega

Protótipo em desenvolvimento na branch separada do fork; não é uma release.
A autorização GitHub foi corrigida e a branch remota criada em 2026-10-19.
24 testes da política de categorias passaram usando o lançador de código-fonte Java.
A compilação completa é validada pelo GitHub Actions com Java 25.
Os testes no mundo singleplayer, em servidor dedicado e a integração Geyser/Floodgate continuam pendentes.
Em 2026-10-19 o primeiro teste em singleplayer revelou que a declaração `environment: server` fazia o Fabric ignorar o mod ao abrir o cliente. Corrigido para `environment: *` (o comportamento só altera lógica de jogo do lado do servidor, inclusive o servidor integrado).

## Como usar depois de compilar e validar

- Origem: baú de cobre, como no original.
- Moldura comum ou brilhante na posição original: filtro exato, incluindo componentes.
- Um giro (45 graus, rotação interna 1): categoria automática.
- Demais rotações (2 a 7): filtro exato. A regra independe do tipo de moldura.
- Madeira: troncos, tábuas e derivados identificados pelas tags vanilla de madeira.
- Pedra: conjunto explícito de pedras de construção e variantes de escadas, lajes e muros.
- Minerais: minérios vanilla, materiais brutos, lingotes, pepitas, gemas e blocos listados.
- Alimentos: itens com componente FOOD; não inclui automaticamente trigo ou bolo em bloco.
- Lã: lã e carpetes pela tag vanilla.
- Vidro: vidro, painéis, vidro tingido e escurecido.
- Uma categoria governa o recipiente inteiro, inclusive as duas metades de um baú duplo.
- Categorias conflitantes ou representante desconhecido em modo categoria bloqueiam o destino.
- Se houver categoria válida, molduras exatas adicionais não ampliam essa categoria.
- Sem filtro preenchido: continua disponível como armazenamento de sobra.

Observação de migração: uma moldura que já esteja na rotação 1 passará
a ativar categorias. Volte à rotação zero para preservar seu filtro exato.
A classificação não cobre todos os itens do jogo: itens não classificados continuam
utilizáveis em filtros exatos. Quartz é mineral; quartz_block é pedra de construção.
Madeira tratada e comida de outros mods podem entrar por tags/componentes existentes;
nomes de outros namespaces não são classificados por suposição.

## Inspeção do original

Já existem: origem em baú de cobre, destinos em baús normais e armadilha,
filtros exatos, combinação de inventários de baús duplos, armazenamento de sobra,
retorno à origem, bloqueios entre golems, busca horizontal 32 e vertical 8,
e depósito remoto após ficar preso por 100 ticks em um destino filtrado.
O fluxo ativo implementa seus próprios filtros; ChestRegistry/ChestScanner não
constituem o mecanismo ativo de seleção de destinos.

Corrigido neste protótipo: molduras vizinhas só são compartilhadas entre parceiros
reais de baú duplo; os filtros são conferidos novamente antes de inserir itens.
O empilhamento continua exigindo igualdade exata de item e componentes.

## Fora do escopo desta alteração inicial

Barris ainda não são destinos. Caixas de shulker foram retiradas do planejamento por decisão do projeto; não serão integradas. A busca vertical existente NÃO prova
interação a quatro blocos: isso exige mudar pontos de aproximação e testar alcance.
O depósito remoto preexistente foi preservado e precisa ser revisado nessa etapa.
Os bloqueios estáticos do original foram corrigidos para incluir dimensão e dono da reserva; a melhoria foi compilada com sucesso, mas ainda requer testes com vários golems.
Animação de barris e pernas telescópicas permanecem adiadas.

## Etapa 2 — otimização experimental (branch `feature/ai-optimization-26.2`)

Após o usuário validar as seis categorias no singleplayer, a etapa 2 preserva todos os filtros e
substitui a busca exaustiva de 71.825 posições de blocos por uma inspeção das posições de
block entities em chunks já carregados. Os candidatos continuam dentro dos mesmos limites
horizontais e verticais e são ordenados pela mesma distância, sem carregar chunks adicionais.
O custo efetivo passa a depender dos recipientes e demais block entities presentes nesses chunks,
não da quantidade de blocos de ar, pedra ou terra. Há testes JUnit dos limites negativos e inclusivos.
**A compilação não garante melhora de FPS/TPS:** é necessário comparar em mundos de teste.

Testar antes de integrar barris (etapa 3):
1. Repetir minerais e madeira com uma única moldura 45°, depois os outros quatro filtros.
2. Conferir baú duplo, filtros exatos (0°), baú sem filtro e prioridade do baú filtrado.
3. Testar baús na borda de chunks, coordenadas negativas, 32 blocos na horizontal e 8 na vertical.
4. Testar 1, 5 e 10 golems no mesmo mundo, comparando fluidez com a versão anterior.
5. Conferir que chunks distantes NÃO são forçados a carregar pela busca.
6. Repetir com o mundo salvo e reaberto; verificar contagem de itens e ausência de duplicação.
7. Confirmar o funcionamento tanto em singleplayer quanto em servidor dedicado.

Próxima integração após os testes: **barris**, sem suporte planejado a shulker boxes.

## Compilação e testes necessários

O workflow proposto fica em `.github/workflows/fabric-build.yml` na raiz,
executa os testes da política e `bash gradlew --no-daemon build` dentro de Fabric
com Java 25. Só gera artefatos de CI; não publica releases nem faz merge.
O workflow original em `Fabric/.github/workflows` não era descoberto pelo GitHub.
As versões upstream foram preservadas, incluindo Loom SNAPSHOT: resolução precisa
ser confirmada no primeiro build. LICENSE e NOTICE entram no JAR.

Antes de considerar funcional, executar em um mundo de teste:

1. Iniciar um mundo singleplayer Fabric 26.2 com o JAR na pasta de mods do cliente e Fabric API; no `latest.log`, confirmar `smart-copper-golem` entre os mods carregados e a mensagem `Copper Golems are smart now!`.
2. Iniciar servidor dedicado Fabric 26.2 com o mesmo JAR e Fabric API; verificar mixins e conectar um cliente Java sem o mod.
3. Confirmar filtros exatos com itens renomeados/encantados e diferentes componentes.
4. Para cada categoria, transportar dois itens diferentes e rejeitar outra categoria.
5. Testar ambos os lados do baú duplo; dois baús simples vizinhos não compartilham filtro.
6. Testar conflitos entre categorias, representante desconhecido, moldura vazia e removida.
7. Trocar uma moldura durante o trajeto: o depósito deve respeitar o filtro atualizado.
8. Testar baú cheio, parcialmente cheio, sobra sem filtro e múltiplos golems.
9. Contar os itens antes/depois, incluindo reinício e descarga de chunks; não pode duplicar/perder.
10. Repetir interação com molduras e transporte usando cliente Bedrock via Geyser/Floodgate,
    registrando versões de servidor, clientes e ponte. Compatibilidade ainda NÃO comprovada.

A instalação do lado Fabric é declarada `*` para permitir que o mod carregue tanto
em servidores dedicados quanto no cliente que hospeda o mundo singleplayer (servidor integrado).
Ao usar servidor dedicado, a intenção é instalar o JAR somente no servidor; nenhum item,
entidade, pacote ou recurso novo de cliente foi introduzido. A compatibilidade com Geyser/Floodgate
não está comprovada e exige os testes acima.

Créditos originais e licença Apache 2.0 preservados. Modificações: Kesley, 2026-10-19.
