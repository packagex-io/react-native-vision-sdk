import React, { useCallback, useState } from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal, Alert } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';


const TemplateSelectionView = ({
  templates = [],
  isVisible = true,
  setIfVisible = (val: boolean) => { },
  selectedTemplate,
  setSelectedTemplate,
  onPressCreateTemplate,
  onPressDeleteTemplateById,
  onPressDeleteAllTemplates
}) => {

  const closeModal = useCallback(() => {
    setIfVisible(false);
  }, []);

  const handlePressOnCreate = () => {
    onPressCreateTemplate()
    closeModal()
  }

  const handleSelectTemplate = useCallback((template) => {
    if (selectedTemplate?.id === template?.id) {
      setSelectedTemplate(null)
    } else {
      setSelectedTemplate(template)
    }
    closeModal()
  }, [selectedTemplate, closeModal, setSelectedTemplate])


  return (
    <Modal
      animationType="fade"
      transparent
      visible={isVisible}
      onRequestClose={closeModal}
    >
      <TouchableOpacity
        activeOpacity={1}
        onPress={closeModal}
        style={styles.centeredViewModal}
      >
        <View style={styles.modalView}>
          <TouchableOpacity
            onPress={handlePressOnCreate}
            style={[styles.rowStyle, { justifyContent: 'center', backgroundColor: 'rgba(0, 239, 0, 0.8)', width: '80%', marginHorizontal: 'auto', borderRadius: 12, height: 38, marginVertical: 8 }]}
          >
            <Text style={[styles.textStyle, { fontWeight: 'bold' }]}>Create Template</Text>
          </TouchableOpacity>
          {templates.map((item, i) =>

            <React.Fragment key={item.id || i}>
              <View style={styles.horizontalLine} />
              <View
                style={styles.rowStyle}
              >
                <TouchableOpacity onPress={() => onPressDeleteTemplateById(item.id)}>
                  <MaterialIcons name="delete-outline" size={20} color="#e32d2d" />
                </TouchableOpacity>
                <TouchableOpacity
                  onPress={() => handleSelectTemplate(item)}
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    flexGrow: 1,
                    justifyContent: 'space-between',
                    marginLeft: 12,
                    height: '100%',
                    // backgroundColor:'yellow'
                  }}>
                  <Text style={[styles.textStyle]}>{item.id}</Text>

                  {selectedTemplate?.id == item.id && (
                    <MaterialIcons name="done" size={20} color="rgba(0, 239, 0, 0.8)" />
                  )}
                </TouchableOpacity>

              </View>

            </React.Fragment>
          )}

          <View style={styles.horizontalLine} />
          <TouchableOpacity
            onPress={() => handleSelectTemplate(null)}
            style={[styles.rowStyle]}
          >
            <Text style={styles.textStyle}>None</Text>
            {!selectedTemplate?.id && (
              <MaterialIcons name="done" size={20} color="rgba(0, 239, 0, 0.8)" />
            )}

          </TouchableOpacity>

          {templates.length ?
            <React.Fragment>
              <View style={styles.horizontalLine} />
              <TouchableOpacity
                onPress={onPressDeleteAllTemplates}
                style={[styles.rowStyle, { backgroundColor: '#e32d2d', justifyContent: 'center', borderRadius: 12, width: '80%', margin: 'auto', height: 38, marginVertical: 8 }]}
              >
                <Text style={[styles.textStyle, { fontWeight: 'bold' }]}>Delete All Templates</Text>

              </TouchableOpacity>
            </React.Fragment> : null}
        </View>
      </TouchableOpacity>
    </Modal>
  );
}
const styles = StyleSheet.create({
  centeredViewModal: {
    // backgroundColor: 'red',
    flex: 1,
    marginRight: 10,
    alignItems: 'flex-start',
    justifyContent: 'flex-start',
    width: '100%',
    position: 'relative'
    // left: 100,
    // top: 100,
  },
  modalView: {
    position: 'absolute',
    right: 60,
    top: '22%',
    backgroundColor: '#33343A',
    borderRadius: 12,
    paddingVertical: 10,
    width: '55%',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  horizontalLine: {
    height: 1,
    width: '100%',
    backgroundColor: '#4D4D57',
    marginVertical: 5,
  },
  rowStyle: {
    height: 48,
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
  },
  textStyle: {
    fontSize: 14,
    color: 'white',
  },
});

export default TemplateSelectionView;
