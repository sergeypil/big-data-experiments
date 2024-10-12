import psycopg2
import subprocess
from datetime import datetime

from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from airflow import DAG
from airflow.models import Connection

def install_dependencies():
    subprocess.run(['pip','install','psycopg2'])

def create_database_with_tables():
    connectionConfig = Connection.get_connection_from_secrets("conn1")
    conn = psycopg2.connect(database=connectionConfig.schema,user=connectionConfig.login,password=connectionConfig.password,
                            host=connectionConfig.host,port=connectionConfig.port,sslmode=connectionConfig.extra_dejson["sslmode"])
    print('DB connected successfully')

    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE metrics.metrics (
          "componentname" text,
          "fromtimestamp" text,
          "maxvalue" double precision,
          "metricname" text,
          "minvalue" double precision,
          "totimestamp" text,
          unit text
        );
    """)

    conn.commit()
    conn.close()

database_init_dag = DAG(dag_id='database-init',
                         description='DAG for initialising a Postgre database',
                         schedule_interval=None,
                         start_date=datetime(2024,1,4))

task0 = PythonOperator(task_id='Install-dependencies',
                       python_callable=install_dependencies,
                       dag=database_init_dag)
task1 = PythonOperator(task_id='Create-Database-With-Tables',
                       python_callable=create_database_with_tables,
                       dag=database_init_dag)
