package com.example.ipos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.epson.eposprint.*;
import com.epson.epsonio.*;
import com.db.Model;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class ActivityImprimirVentas extends Activity implements View.OnClickListener {
    EditText etimpresora;
    Button btbuscarimp;
    TextView tvticket;
    Button btimprimirventa;
    Print impresora;
    Model dbmanager;
    Integer factura;
    String razonsocial;
    AlertDialog.Builder builder;
    String ruccliente;
    String condicion;
    String fec_vto;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imprimir_ventas);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        dbmanager = new Model(this);

        etimpresora = (EditText) findViewById(R.id.etimpresora);
        btbuscarimp = (Button) findViewById(R.id.btbuscarimp);
        tvticket = (TextView) findViewById(R.id.tvticket);
        btimprimirventa = (Button) findViewById(R.id.btimprimirventa);
        btbuscarimp.setOnClickListener(this);
        btimprimirventa.setOnClickListener(this);
        obtenerimpresora();
        impresora = new Print(getApplicationContext());
        Bundle extras = getIntent().getExtras();
        if(extras !=null){
            String value = extras.getString("numero");
            factura = Integer.parseInt(value);
            razonsocial = extras.getString("razonsocial");
            ruccliente =  extras.getString("ruccliente");
            condicion = extras.getString("condicion");
            fec_vto = extras.getString("fec_vto");
        }

        builder = new AlertDialog.Builder(this);
        builder.setTitle("Mensaje");
        builder.setCancelable(true);
        builder.setNeutralButton("Aceptar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        crearTicket();
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btbuscarimp){
            obtenerimpresora();
        }else if (view.getId() == R.id.btimprimirventa){
            iniciarImpresion();
            try {
                impresora.closePrinter();
            } catch (EposException e) {
                e.printStackTrace();
            }
            setResult(25);
            dbmanager.close();
            this.finish();

        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_imprimir_ventas, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                try {
                    Finder.stop();
                }catch(Exception e){

                }

                try {
                    impresora.closePrinter();
                } catch (EposException e) {
                    e.printStackTrace();
                }
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Finder.stop();
        }catch(Exception e){
            return;
        }

        try {
            impresora.closePrinter();
        } catch (EposException e) {
            e.printStackTrace();
        }
    }
    public void obtenerimpresora(){

        int errStatus = IoStatus.SUCCESS;
        int intentos = 1;
        String[] mList = null;
        //Start search
        while(etimpresora.getText().length() < 1) {
            System.out.print("Intentando conexion");
            try {
                Finder.stop();
            } catch (Exception e) {

            }

            try {
                Finder.start(this, DevType.USB, null);
                //Exception handling
            } catch (Exception e) {
                ShowMsg.showException(e, "start", this);
                return;
            }

            try {
                mList = Finder.getResult();
                //Exception handling
            } catch (Exception e) {
                ShowMsg.showException(e, "finder", this);
                return;
            }
            try {
                //etimpresora.setText(mList[0].toString());
                for (int i = 0; i < mList.length; i++) {
                    etimpresora.setText(mList[i]);

                }
            } catch (Exception e) {
                ShowMsg.showException(e, "Result", this);
                return;
            }

            if (intentos == 100){
                etimpresora.setText("Imp. no conectado");
            }
            intentos = intentos + 1;
        }

        if (etimpresora.getText().toString() == "Imp. no conectado"){
            AlertDialog msg;
            msg = builder.create();
            msg.setMessage("La impresora no responde.");
            msg.show();
        }

    }

    public void conectarImpresora() {
        //open
        try{
            impresora.openPrinter(Print.DEVTYPE_USB, etimpresora.getText().toString(), Print.FALSE, 1000);
        }catch(EposException e){
            //impresora.closePrinter();
            impresora = null;
            ShowMsg.showException(e, "openPrinter" , this);
            return;
        }
    }

    public void iniciarImpresion(){
        int[] status = new int[1];
        status[0] = 0;
        int[] battery = new int[1];
        battery[0] = 0;
        try {
            Builder builder = new Builder("TM-U220", Builder.MODEL_ANK);

            builder.addTextLang(Builder.LANG_EN);
            //builder.addTextSmooth(Builder.TRUE);
            //builder.addTextFont(Builder.FONT_A);
            //builder.addTextSize(1, 1);
            //builder.addTextStyle(Builder.FALSE, Builder.FALSE, Builder.TRUE, Builder.PARAM_UNSPECIFIED);


            builder.addText(tvticket.getText().toString());
            conectarImpresora();


            impresora.sendData(builder, 1000, status);
            //<End communication with the printer>
            ShowMsg.showStatus(EposException.SUCCESS, status[0], battery[0], this);
            try {
                Finder.stop();
            }catch(Exception e){
                return;
            }
            impresora.closePrinter();

        } catch (Exception e) {
            ShowMsg.showException(e, "Printer" , this);
            return;
        }
    }


    public void crearTicket(){
        //obteniendo el encabezado del documento
        String ticket =  "";
        String empresa;
        String est;
        String puntoexp;
        String act_eco = "DISTRIBUIDORA";
        String ruc;
        String telefono;
        String timbrado;
        String vencimiento;
        Integer total = 0;
        Integer ex = 0;
        Integer g5 = 0;
        Integer g10 = 0;
        String separador = "- - - - - - - - - -- - - - - - - - - - -" + "\n";
        ArrayList<String> pex = dbmanager.getAllPex();
        empresa = pex.get(4).toString().toUpperCase();
        est = pex.get(5);
        puntoexp = pex.get(6);
        ruc = pex.get(7).toString();
        telefono = pex.get(8).toString();
        timbrado = pex.get(0).toString();
        vencimiento = pex.get(2).toString();
        //empresa
        ticket = ticket + StringUtil.justifyCenter(empresa, 40, ' ') + "\n";
        //actividad económica
        ticket = ticket + StringUtil.justifyCenter(act_eco, 40, ' ') + "\n";
        //ruc
        ticket = ticket + StringUtil.justifyCenter("RUC: " + "XXXXXXX", 40, ' ') + "\n";
        //telefono
        ticket = ticket + StringUtil.justifyCenter(telefono, 40, ' ') + "\n";
        //direccion
        ticket = ticket + StringUtil.justifyCenter("DIRECCION ESTABLECIMIENTO", 40, ' ') + "\n";
        //timbrado
        ticket = ticket + StringUtil.justifyCenter("TIMBRADO NRO.: " + "XXXXXXX", 40, ' ') + "\n";
        //validez
        ticket = ticket + StringUtil.justifyCenter("VALIDEZ " + "XX/XX/XXXX", 40, ' ') + "\n";
        //ciudad
        ticket = ticket + StringUtil.justifyCenter("DR. J. M. FRUTOS-PARAGUAY", 40, ' ') + "\n";
        //iva incluido
        ticket = ticket + StringUtil.justifyCenter("IVA INCLUIDO", 40, ' ') + "\n";
        ticket = ticket + separador;

        //numero de factura
        ticket =  ticket + String.format("%-40s", "FACTURA NRO.: " + String.format("%03d", Integer.parseInt(est))
                + "-" + String.format("%03d", Integer.parseInt(puntoexp)) + "-" + String.format("%07d", factura) ) + "\n";

        //fecha de emision del documento
        ticket = ticket + String.format("%-40s", "FECHA: " + getDate() + " " + getTime()) + "\n";
        //condicion de venta   #########OBTENER VALOR DEL SPINNER DE SELECCION############
        ticket = ticket + String.format("%-40s", "CONDICION DE VENTA: "+ " CONTADO ") + "\n";

        //vendedor ##########OBTENER DATOS DEL VENDEDOR ACTUAL################
        ticket = ticket + String.format("%-40s", "VENDEDOR: " + "PRUEBA") + "\n";
        ticket = ticket + separador;

        //cliente
        ticket = ticket + String.format("%-40s", "RAZON SOCIAL: " + razonsocial) + "\n";
        //ruc o numero de documento de indentidad
        ticket = ticket + String.format("%-40s", "RUC O CI NRO.: " + ruccliente) + "\n";
        ticket = ticket + separador;

        Cursor res = dbmanager.getDetalleFac(factura.toString(), timbrado);
        res.moveToFirst();
        String row = "";
        while(res.isAfterLast() == false){

            row = row + String.format("%07d", Integer.parseInt(res.getString(0))) + " ";
            if (res.getString(1).length() > 28){
                row = row + String.format("%-28s", res.getString(1).substring(0, 27)) + " ";
            }else{
                row = row + String.format("%-28s", res.getString(1)) + " ";
            }
            row = row + String.format("%-2s", res.getString(2)) + "%" + "\n";

            row = row + String.format("%-6s", res.getString(3)) + " ";
            row = row + String.format("%-7s", res.getString(4) + "/" + res.getString(5)) + " ";
            row = row + String.format("%-12s", res.getInt(6));
            row = row + String.format("%13s", res.getInt(7)) + "\n";

            //sumando los totales
            //total = total + Integer.parseInt(res.getString(7));
            total = total + res.getInt(7);
            if (Integer.parseInt(res.getString(2)) == 0){
                //ex = ex + Integer.parseInt(res.getString(7));
                ex = ex + res.getInt(7);
            }
            if (Integer.parseInt(res.getString(2)) == 5){
                //g5 = g5 + Integer.parseInt(res.getString(7));
                g5 = g5 + res.getInt(7);
            }
            if (Integer.parseInt(res.getString(2)) == 10){
                //g10 = g10 + Integer.parseInt(res.getString(7));
                g10 = g10 + res.getInt(7);
            }
            res.moveToNext();
        }
        ticket = ticket + row;
        ticket = ticket + separador;
        ticket = ticket + String.format("%-20s", "IMPORTE TOTAL");
        ticket = ticket + String.format("%20s", total.toString()) + "\n";
        ticket = ticket + separador;

        ticket = ticket +  String.format("%-40s", "TOTALES") + "\n";

        ticket = ticket + String.format("%-20s", "EXENTAS");
        ticket = ticket + String.format("%20s", ex.toString()) + "\n";

        ticket = ticket + String.format("%-20s", "GRAVADAS 5% ");
        ticket = ticket + String.format("%20s", g5.toString()) + "\n";

        ticket = ticket + String.format("%-20s", "GRAVADAS 10% ");
        ticket = ticket + String.format("%20s", g10.toString()) + "\n";

        ticket = ticket +  String.format("%-40s", "LIQUIDACION DEL IVA") + "\n";

        ticket = ticket + String.format("%-20s", "IVA 5% ");
        ticket = ticket + String.format("%20s", Math.round(g5/21)) + "\n";

        ticket = ticket + String.format("%-20s", "IVA 10% ");
        ticket = ticket + String.format("%20s", Math.round(g10/11)) + "\n";

        ticket = ticket +  String.format("%40s", "- - - - - - - - - -") + "\n";

        ticket = ticket + String.format("%-20s", "TOTAL IVA ");
        ticket = ticket + String.format("%20s",Math.round(g5/21) + Math.round(g10/11)) + "\n";

        ticket = ticket + separador;

        ticket = ticket + StringUtil.justifyCenter("GRACIAS POR SU PREFERENCIA!", 40, ' ') + "\n";

        ticket = ticket + separador+"\n\n\n\n\n\n";

        tvticket.setText(ticket);

    }

    private String getDate() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd/MM/yyyy", Locale.getDefault());
        return dateFormat.format(cal.getTime());
    }

    private String getTime() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "HH:mm:ss", Locale.getDefault());
        return dateFormat.format(cal.getTime());
    }
}
