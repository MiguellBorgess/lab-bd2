#!/bin/bash
export PGPASSWORD="postgres"

Q_INTERSECT="select nome_cliente, numero_conta, nome_agencia from emprestimo intersect select nome_cliente, numero_conta, nome_agencia from deposito;"
Q_JOIN="SELECT DISTINCT e.nome_cliente, e.numero_conta, e.nome_agencia FROM emprestimo e JOIN deposito d ON e.nome_cliente = d.nome_cliente AND e.numero_conta = d.numero_conta AND e.nome_agencia = d.nome_agencia;"
Q_DISTINCT="SELECT DISTINCT e.nome_cliente, e.numero_conta, e.nome_agencia FROM emprestimo e, deposito d WHERE e.nome_cliente = d.nome_cliente AND e.numero_conta = d.numero_conta AND e.nome_agencia = d.nome_agencia;"

echo "Executando..."

echo "intersect,join,distinct" > resultados.csv

for i in {1..30}; do
    T1=$(psql -h localhost -p 5432 -U postgres -d IB -t -c "EXPLAIN ANALYZE $Q_INTERSECT" | grep "Execution Time" | awk '{print $3}')
    T2=$(psql -h localhost -p 5432 -U postgres -d IB -t -c "EXPLAIN ANALYZE $Q_JOIN" | grep "Execution Time" | awk '{print $3}')
    T3=$(psql -h localhost -p 5432 -U postgres -d IB -t -c "EXPLAIN ANALYZE $Q_DISTINCT" | grep "Execution Time" | awk '{print $3}') 
    echo "Rodada $i: INTERSECT=${T1}ms | JOIN=${T2}ms | DISTINCT=${T3}ms"
    echo "$T1,$T2,$T3" >> resultados.csv
done

echo "Testes salvos em resultados.csv"
