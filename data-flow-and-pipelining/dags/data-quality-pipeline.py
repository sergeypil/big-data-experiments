import glob
import psycopg2
import subprocess
from datetime import datetime

from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from airflow import DAG
from airflow.models import Connection
from airflow.providers.dbt.cloud.operators.dbt import DbtCloudRunJobOperator




def install_dependencies():
    subprocess.run(['pip','install','psycopg2'])

def copy_csv_to_table():
    connectionConfig = Connection.get_connection_from_secrets("conn1")
    conn = psycopg2.connect(database=connectionConfig.schema,user=connectionConfig.login,password=connectionConfig.password,
                            host=connectionConfig.host,port=connectionConfig.port,sslmode=connectionConfig.extra_dejson["sslmode"])
    print('DB connected successfully')

    cursor = conn.cursor()
    for file_path in glob.glob('dags/data/*.csv'):
        with open(file_path, 'r') as file:
            cursor.copy_expert("""
                COPY metrics.metrics (componentName,fromTimestamp,maxValue,metricName,minValue,toTimestamp,unit)
                FROM STDIN WITH CSV HEADER DELIMITER ','
            """, file)
        conn.commit()
        print(f'Data from {file_path} copied successfully')


    cursor.close()
    conn.close()

data_quality_pipeline = DAG(dag_id='data-quality-pipeline',
                         description='DAG for populating table',
                         schedule_interval=None,
                         start_date=datetime(2024,1,4))

task0 = PythonOperator(task_id='Install-dependencies',
                       python_callable=install_dependencies,
                       dag=data_quality_pipeline)
task1 = PythonOperator(task_id='Copy-csv',
                       python_callable=copy_csv_to_table,
                       dag=data_quality_pipeline)
task2 = DbtCloudRunJobOperator(
           task_id="trigger_dbt_cloud_job_run",
           dbt_cloud_conn_id="conn_dbt",
           job_id=70403104215068,
           check_interval=10,
           timeout=300,
           dag=data_quality_pipeline
       )