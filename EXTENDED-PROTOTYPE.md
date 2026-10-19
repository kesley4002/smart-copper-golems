# Smart Copper Golems Extended — primeiro protótipo

Base: GameMech007, branch `v-26.2`, commit `721ec7728577dcd36359a138afc94b2f3197ba9b`.
Branch pretendida no fork: `feature/smart-categories-26.2`.

## Estado real da entrega

Protótipo em desenvolvimento na branch separada do fork; não é uma release.
A autorização GitHub foi corrigida e a branch remota criada em 2026-10-19.
24 testes da política de categorias passaram usando o lançador de código-fonte Java.
A compilação completa é validada pelo GitHub Actions com Java 25.
Os testes de servidor e integração Geyser/Floodgate continuam pendentes.

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

Barris e shulkers ainda não são destinos. A busca vertical existente NÃO prova
interação a quatro blocos: isso exige mudar pontos de aproximação e testar alcance.
O depósito remoto preexistente foi preservado e precisa ser revisado nessa etapa.
Os bloqueios estáticos do original são por posição sem dimensão; há risco de
contenção entre dimensões, ainda não corrigido nesta alteração focada nos filtros.
Animação de barris e pernas telescópicas permanecem adiadas.

## Compilação e testes necessários

O workflow proposto fica em `.github/workflows/fabric-build.yml` na raiz,
executa os testes da política e `bash gradlew --no-daemon build` dentro de Fabric
com Java 25. Só gera artefatos de CI; não publica releases nem faz merge.
O workflow original em `Fabric/.github/workflows` não era descoberto pelo GitHub.
As versões upstream foram preservadas, incluindo Loom SNAPSHOT: resolução precisa
ser confirmada no primeiro build. LICENSE e NOTICE entram no JAR.

Antes de considerar funcional, executar em um mundo de teste:

1. Iniciar servidor dedicado Fabric 26.2 com o JAR e Fabric API; verificar mixins.
2. Conectar cliente Java sem instalar o mod no cliente.
3. Confirmar filtros exatos com itens renomeados/encantados e diferentes componentes.
4. Para cada categoria, transportar dois itens diferentes e rejeitar outra categoria.
5. Testar ambos os lados do baú duplo; dois baús simples vizinhos não compartilham filtro.
6. Testar conflitos entre categorias, representante desconhecido, moldura vazia e removida.
7. Trocar uma moldura durante o trajeto: o depósito deve respeitar o filtro atualizado.
8. Testar baú cheio, parcialmente cheio, sobra sem filtro e múltiplos golems.
9. Contar os itens antes/depois, incluindo reinício e descarga de chunks; não pode duplicar/perder.
10. Repetir interação com molduras e transporte usando cliente Bedrock via Geyser/Floodgate,
    registrando versões de servidor, clientes e ponte. Compatibilidade ainda NÃO comprovada.

A instalação é declarada `server`; nenhum item, entidade, pacote ou recurso de
cliente novo foi introduzido. Isso favorece a estratégia de compatibilidade,
mas não substitui os testes acima.

Créditos originais e licença Apache 2.0 preservados. Modificações: Kesley, 2026-10-19.
