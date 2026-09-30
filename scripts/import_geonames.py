#!/usr/bin/env python3
import argparse,csv,re,unicodedata,uuid,zipfile,io
from contextlib import contextmanager
from pathlib import Path
NAMESPACE=uuid.UUID('38a5558e-94eb-5d6b-a733-1e5a3758fd1b')
COLUMNS={'id','country_code','postal_code','normalized_postal_code','locality_name','normalized_locality_name','admin_area_1_name','admin_area_1_code','admin_area_2_name','admin_area_2_code','admin_area_3_name','admin_area_3_code','location','accuracy','source'}
def normalize(v):return re.sub(r'[^a-z0-9]+',' ',''.join(c for c in unicodedata.normalize('NFKD',v) if not unicodedata.combining(c)).lower()).strip()
def postal(v):return re.sub(r'[^A-Z0-9]','',v.upper())
def stable_id(country,code,place,a1,a2,a3):return uuid.uuid5(NAMESPACE,'\0'.join((country,code,place,normalize(a1),normalize(a2),normalize(a3))))
@contextmanager
def records(path):
 if path.suffix.lower()=='.zip':
  with zipfile.ZipFile(path) as z:
   names=[n for n in z.namelist() if n.lower().endswith('.txt')]
   if len(names)!=1:raise ValueError('ZIP must contain exactly one .txt')
   with z.open(names[0]) as f:yield csv.reader(io.TextIOWrapper(f,encoding='utf-8'),delimiter='\t')
 elif path.suffix.lower()=='.txt':
  with path.open(encoding='utf-8',newline='') as f:yield csv.reader(f,delimiter='\t')
 else:raise ValueError('input must be .txt or .zip')
def convert(v,country):
 if len(v)!=12:raise ValueError(f'expected 12 columns, got {len(v)}')
 c,pc,place,a1,c1,a2,c2,a3,c3,lat,lng,acc=v;c=c.upper()
 if c!=country or not re.fullmatch(r'[A-Z]{2}',c):raise ValueError('invalid country')
 lat,lng=float(lat),float(lng)
 if not(-90<=lat<=90 and -180<=lng<=180):raise ValueError('coordinates out of range')
 acc=int(acc) if acc else None
 if acc is not None and not 0<=acc<=10:raise ValueError('accuracy out of range')
 pc_n,place_n=postal(pc),normalize(place)
 if not pc_n or not place_n:raise ValueError('postal code and locality required')
 n=lambda x:x or None
 return(stable_id(c,pc_n,place_n,a1,a2,a3),c,pc,pc_n,place,place_n,n(a1),n(c1),n(a2),n(c2),n(a3),n(c3),lng,lat,acc)
SQL='''INSERT INTO search_location(id,country_code,postal_code,normalized_postal_code,locality_name,normalized_locality_name,admin_area_1_name,admin_area_1_code,admin_area_2_name,admin_area_2_code,admin_area_3_name,admin_area_3_code,location,accuracy,source) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,ST_SetSRID(ST_MakePoint(%s,%s),4326)::geography,%s,'GEONAMES') ON CONFLICT(id) DO UPDATE SET postal_code=EXCLUDED.postal_code,normalized_postal_code=EXCLUDED.normalized_postal_code,locality_name=EXCLUDED.locality_name,normalized_locality_name=EXCLUDED.normalized_locality_name,admin_area_1_name=EXCLUDED.admin_area_1_name,admin_area_1_code=EXCLUDED.admin_area_1_code,admin_area_2_name=EXCLUDED.admin_area_2_name,admin_area_2_code=EXCLUDED.admin_area_2_code,admin_area_3_name=EXCLUDED.admin_area_3_name,admin_area_3_code=EXCLUDED.admin_area_3_code,location=EXCLUDED.location,accuracy=EXCLUDED.accuracy,updated_at=CURRENT_TIMESTAMP'''
def main():
 import psycopg
 p=argparse.ArgumentParser();p.add_argument('file',type=Path);p.add_argument('--dsn',required=True);p.add_argument('--country',required=True,type=str.upper);p.add_argument('--batch-size',type=int,default=1000);p.add_argument('--replace-country',action='store_true');a=p.parse_args()
 if a.batch_size<=0 or not re.fullmatch(r'[A-Z]{2}',a.country):p.error('invalid batch size or country')
 with psycopg.connect(a.dsn) as db:
  with db.cursor() as cur:
   cur.execute("SELECT column_name FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='search_location'");missing=COLUMNS-{x[0] for x in cur.fetchall()}
   if missing:raise RuntimeError('missing schema columns: '+','.join(sorted(missing)))
   if a.replace_country:cur.execute('DELETE FROM search_location WHERE country_code=%s',(a.country,))
   batch=[]
   with records(a.file) as rows:
    for line,row in enumerate(rows,1):
     try:batch.append(convert(row,a.country))
     except Exception as e:raise ValueError(f'line {line}: {e}') from e
     if len(batch)>=a.batch_size:cur.executemany(SQL,batch);batch.clear()
    if batch:cur.executemany(SQL,batch)
if __name__=='__main__':main()
