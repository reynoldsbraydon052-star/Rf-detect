import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("onExportCsv = { uri -> viewModel.exportRfEventsCsv(context, uri) }", 
"""onExportCsv = { uri -> viewModel.exportRfEventsCsv(context, uri) },
                            onExportCaptures = { 
                                android.widget.Toast.makeText(context, "No PCAP or raw SDR capture files available for this session.", android.widget.Toast.LENGTH_LONG).show() 
                            }""")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
